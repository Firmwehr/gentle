package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.Relation;
import firm.TargetValue;
import firm.bindings.binding_irgopt;
import firm.bindings.binding_irnode;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.Align;
import firm.nodes.And;
import firm.nodes.Bad;
import firm.nodes.Binop;
import firm.nodes.Bitcast;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Cond;
import firm.nodes.Confirm;
import firm.nodes.Const;
import firm.nodes.Conv;
import firm.nodes.Div;
import firm.nodes.Dummy;
import firm.nodes.Eor;
import firm.nodes.Id;
import firm.nodes.Jmp;
import firm.nodes.Load;
import firm.nodes.Member;
import firm.nodes.Minus;
import firm.nodes.Mod;
import firm.nodes.Mul;
import firm.nodes.Mulh;
import firm.nodes.Mux;
import firm.nodes.NoMem;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Not;
import firm.nodes.Offset;
import firm.nodes.Or;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Shl;
import firm.nodes.Shr;
import firm.nodes.Shrs;
import firm.nodes.Size;
import firm.nodes.Start;
import firm.nodes.Store;
import firm.nodes.Sub;
import firm.nodes.Unknown;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BinaryOperator;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class ConstantFolding extends NodeVisitor.Default {
	private static final Logger LOGGER = new Logger(ConstantFolding.class);

	private final Graph graph;
	// stores associated lattice element for each visited note during fixed point iteration
	private final Map<Node, TargetValue> constants;
	private final Deque<Node> worklist;

	// keeps track of chages in each iteration until fixed point is reached
	private boolean hasChangedInCurrentIteration;

	public ConstantFolding(Graph graph) {
		this.graph = graph;
		this.worklist = new ArrayDeque<>();
		this.constants = new HashMap<>();
	}

	public static GraphOptimizationStep<Graph, Boolean> constantFolding() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("ConstantFolding")
			.withOptimizationFunction(graph -> {
				int runs = 0;
				while (true) {
					// Needs to be done in each iteration apparently?
					BackEdges.enable(graph);

					ConstantFolding folding = new ConstantFolding(graph);
					folding.fold();
					binding_irgopt.remove_bads(graph.ptr);
					binding_irgopt.remove_unreachable_code(graph.ptr);

					// remove_bads likes to disable back edges if it modifies the graph
					// but we still need them!
					if (!BackEdges.enabled(graph)) {
						BackEdges.enable(graph);
					}
					// This should be done *after* we have removed unreachable code and bads to ensure it doesn't get
					// confused with bad preds
					folding.optimizeBlockChains();

					// testing has shown that back edges get disabled anyway for some reason, but we don't like
					// problems
					BackEdges.disable(graph);

					if (!folding.hasChangedInCurrentIteration) {
						break;
					}
					runs++;
				}
				boolean changed = runs > 0;
				if (changed) {
					dumpGraph(graph, "cf");
				}
				return changed;
			})
			.build();
	}

	private void fold() {
		LOGGER.debugHeader("Analyzing");

		graph.walkTopological(new Default() {
			@Override
			public void defaultVisit(Node n) {
				worklist.addFirst(n);
			}
		});

		while (!worklist.isEmpty()) {
			Node node = worklist.pollLast();
			node.accept(this);
		}

		LOGGER.debugHeader("Folding");

		boolean hasChangedInAnyIteration = this.hasChangedInCurrentIteration;
		for (var entry : constants.entrySet()) {
			this.hasChangedInCurrentIteration = false;

			TargetValue tarVal = entry.getValue();
			Node node = entry.getKey();
			LOGGER.debug("Considering %-25s with tarval %s", node, debugTarVal(tarVal));
			if (!tarVal.isConstant()) {
				continue;
			}

			// Replacing const with const is meaningless and causes infinite iterations as hasChanged is always true
			if (tarVal.getMode().isInt() && node.getOpCode() != binding_irnode.ir_opcode.iro_Const) {
				LOGGER.debug("Replacing   %-25s with tarval %s", node, debugTarVal(tarVal));

				// Add -> Const
				if (node instanceof Div div) {
					replace(div, div.getMem(), graph.newConst(tarVal));
				} else if (node instanceof Mod mod) {
					replace(mod, mod.getMem(), graph.newConst(tarVal));
				} else {
					Graph.exchange(node, graph.newConst(tarVal));
				}
				hasChangedInCurrentIteration = true;
			} else if (node instanceof Cond cond) {
				replaceCondition(tarVal.isOne(), cond);
			}

			if (hasChangedInCurrentIteration && LOGGER.isDebugEnabled()) {
				dumpGraph(graph, "cf-fold");
			}
			hasChangedInAnyIteration |= hasChangedInCurrentIteration;
		}
		this.hasChangedInCurrentIteration = hasChangedInAnyIteration;
	}

	/**
	 * This replaces a condition node with an unconditional jump.
	 *
	 * @param constantValue if true the condition is always true, if false it is always false
	 * @param node the condition node
	 */
	private void replaceCondition(boolean constantValue, Cond node) {
		hasChangedInCurrentIteration = true;

		List<Node> nodes = Util.outsStream(node).toList();

		if (nodes.size() != 2) {
			throw new InternalCompilerException("Expected two nodes for Cond " + node + ", got " + nodes);
		}

		Node trueProj = ((Proj) nodes.get(0)).getNum() == Cond.pnTrue ? nodes.get(0) : nodes.get(1);
		Node falseProj = ((Proj) nodes.get(0)).getNum() == Cond.pnFalse ? nodes.get(0) : nodes.get(1);

		BackEdges.Edge trueEdge = BackEdges.getOuts(trueProj).iterator().next();
		Block trueBlock = (Block) trueEdge.node;
		BackEdges.Edge falseEdge = BackEdges.getOuts(falseProj).iterator().next();
		Block falseBlock = (Block) falseEdge.node;

		if (constantValue) {
			LOGGER.debug("Killing     %-25s| jumping unconditionally to %s", falseBlock, trueBlock);
			trueBlock.setPred(trueEdge.pos, graph.newJmp(node.getBlock()));
			falseBlock.setPred(falseEdge.pos, graph.newBad(Mode.getX()));
		} else {
			LOGGER.debug("Killing     %-25s| jumping unconditionally to %s", trueBlock, falseBlock);
			falseBlock.setPred(falseEdge.pos, graph.newJmp(node.getBlock()));
			trueBlock.setPred(trueEdge.pos, graph.newBad(Mode.getX()));
		}

		// Infinite loops need to be reachable from the end block, this ensures that is the case
		graph.keepAlive(node.getBlock());
	}

	/**
	 * Optimizes blocks with a single predecessor.
	 */
	private void optimizeBlockChains() {
		LOGGER.debugHeader("Optimizing block chains");
		// We can not use walkBlocks as we exchange blocks while considering them, so we need to roll that ourselves
		graph.incBlockVisited();

		Queue<Block> workQueue = new ArrayDeque<>();
		workQueue.add(graph.getEndBlock());

		while (!workQueue.isEmpty()) {
			Block block = workQueue.poll();
			if (block.blockVisited()) {
				continue;
			}
			block.markBlockVisited();

			for (Node pred : block.getPreds()) {
				workQueue.add((Block) pred.getBlock());
			}

			// This might exchange the block, so we need to collect the preds before it
			tryMergeBlock(block);
			deleteIfEmpty(block);
		}
	}

	private void deleteIfEmpty(Block block) {
		for (int i = 0; i < block.getPredCount(); i++) {
			Node pred = block.getPred(i);
			// Only exit from pred block must be a jump
			if (pred.getOpCode() != binding_irnode.ir_opcode.iro_Jmp) {
				continue;
			}
			Block predBlock = (Block) pred.getBlock();
			if (BackEdges.getNOuts(predBlock) != 1) {
				continue;
			}
			if (predBlock.getPredCount() != 1) {
				continue;
			}
			block.setPred(i, predBlock.getPred(0));
			LOGGER.debug("deletable %s in the middle of %s and %s", predBlock, block, predBlock.getPred(0));
		}
	}

	/**
	 * Merges blocks with their predecessor if they have a single input and the pred has an unconditional jump to them:
	 *
	 * <pre>
	 *   ◄────┐  ▲  ┌───►                   ◄────┐  ▲  ┌───►
	 *        │  │  │                            │  │  │
	 *        │  │  │                            │  │  │
	 *      ┌─┴──┴──┴─┐                        ┌─┴──┴──┴─┐
	 *      │         │                        │         │
	 *      │ ┌─────┐ │                        │ ┌─────┐ │
	 *      │ │ JMP │ │    ───────────────►    │ │ RND │ │
	 *      │ └──▲──┘ │                        │ └─────┘ │
	 *      │    │    │                        │         │
	 *      └────┼────┘                        └─────────┘
	 *           │
	 *           │
	 *      ┌────┴────┐
	 *      │         │                           __  __
	 *      │ ┌─────┐ │                           \ \/ /
	 *      │ │ RND │ │    ───────────────►        \  /
	 *      │ └─────┘ │                            /  \
	 *      │         │                           /_/\_\
	 *      └─────────┘
	 * </pre>
	 */
	private void tryMergeBlock(Block block) {
		if (block.getPredCount() != 1) {
			return;
		}
		Node pred = block.getPred(0);

		// Only exit from pred block must be a jump
		if (pred.getOpCode() != binding_irnode.ir_opcode.iro_Jmp) {
			return;
		}

		LOGGER.debug("Merging %-25s with %-25s", block, pred.getBlock());
		Graph.exchange(block, pred.getBlock());
		if (LOGGER.isDebugEnabled()) {
			dumpGraph(graph, "cf-merged");
		}
		hasChangedInCurrentIteration = true;
	}

	@Override
	public void visit(Add node) {
		updateTarVal(node, TargetValue::add);
	}

	@Override
	public void visit(And node) {
		updateTarVal(node, TargetValue::and);
	}

	@Override
	public void visit(Bad node) {
		updateTarVal(node, TargetValue.getBad());
	}

	@Override
	public void visit(Cmp node) {
		TargetValue lhs = tarValOf(node.getLeft());
		TargetValue rhs = tarValOf(node.getRight());

		if (!(lhs.isConstant() && rhs.isConstant())) {
			updateTarVal(node, TargetValue.getUnknown());
			return;
		}

		Relation relation = lhs.compare(rhs);
		if (node.getRelation().contains(relation)) {
			updateTarVal(node, TargetValue.getBTrue());
		} else {
			updateTarVal(node, TargetValue.getBFalse());
		}
	}

	@Override
	public void visit(Cond node) {
		updateTarVal(node, tarValOf(node.getSelector()));
	}

	@Override
	public void visit(Conv node) {
		updateTarVal(node, tarValOf(node.getOp()));
	}

	@Override
	public void visit(Const node) {
		updateTarVal(node, node.getTarval());
	}

	@Override
	public void visit(Div node) {
		updateTarValDivOrMod(node, node.getLeft(), node.getRight(), TargetValue::div);
	}

	private void updateTarValDivOrMod(Node node, Node left, Node right, BinaryOperator<TargetValue> op) {
		if (updateTarVal(node, combineBinOp(left, right, op))) {
			for (BackEdges.Edge out : BackEdges.getOuts(node)) {
				// Do not expand memory nodes
				if (!out.node.getMode().equals(Mode.getM())) {
					for (BackEdges.Edge edge : BackEdges.getOuts(out.node)) {
						worklist.add(edge.node);
					}
				}
			}
		}
	}

	/**
	 * <pre>
	 * 	  Div
	 * 	 /   \
	 * 	M    Res
	 * </pre>
	 * Mod and Div have side effects on memory, we can't just replace them like everything else. Instead, we need to
	 * rewire their memory and output projections.
	 *
	 * @param node the div/mod node to replace
	 * @param previousMemory the memory input of the node
	 * @param replacement the replacement (maybe constant) node
	 */
	private void replace(Node node, Node previousMemory, Node replacement) {
		for (BackEdges.Edge out : BackEdges.getOuts(node)) {
			if (out.node.getMode().equals(Mode.getM())) {
				Graph.exchange(out.node, previousMemory);
			} else {
				Graph.exchange(out.node, replacement);
			}
		}
	}

	@Override
	public void visit(Id node) {
		updateTarVal(node, tarValOf(node.getPred()));
	}

	@Override
	public void visit(Member node) {
		// FIXME: Same as array, should we do this?
		// Check if value remains unchanged between assignment and usage
	}

	@Override
	public void visit(Minus node) {
		TargetValue targetValue = tarValOf(node.getOp());
		if (targetValue.isConstant()) {
			targetValue = targetValue.neg();
		}
		updateTarVal(node, targetValue);
	}

	@Override
	public void visit(Mod node) {
		updateTarValDivOrMod(node, node.getLeft(), node.getRight(), TargetValue::mod);
	}

	@Override
	public void visit(Mul node) {
		updateTarVal(node, TargetValue::mul);
	}

	@Override
	public void visit(Mux node) {
		if (!(node.getFalse() instanceof Const) || !(node.getTrue() instanceof Const)) {
			throw new InternalCompilerException("Unexpected mux arguments");
		}

		if (tarValOf(node.getSel()).equals(TargetValue.getBTrue())) {
			updateTarVal(node, tarValOf(node.getTrue()));
		} else if (tarValOf(node.getSel()).equals(TargetValue.getBFalse())) {
			updateTarVal(node, tarValOf(node.getFalse()));
		}
	}

	@Override
	public void visit(Not node) {
		TargetValue targetValue = tarValOf(node.getOp());
		if (targetValue.isConstant()) {
			targetValue = targetValue.not();
		}
		updateTarVal(node, targetValue);
	}

	@Override
	public void visit(Or node) {
		updateTarVal(node, TargetValue::or);
	}

	@Override
	public void visit(Phi node) {
		// https://cdn.discordapp.com/attachments/900838391560671323/916067791973519440/unknown.png
		TargetValue targetValue = Util.predsStream(node).map(this::tarValOf).reduce(this::φΚοµβαιν).orElseThrow();

		updateTarVal(node, targetValue);
	}

	@Override
	public void visit(Proj node) {
		// Some nodes (like Div) have multiple outgoing projections. If the Div is constant, we do not want to replace
		// the memory output node with the Div constant - that makes no sense!
		if (node.getMode().equals(Mode.getM())) {
			return;
		}
		updateTarVal(node, tarValOf(node.getPred()));
	}

	@Override
	public void visit(Return node) {
	}

	@Override
	public void visit(Shl node) {
		updateTarVal(node, TargetValue::shl);
	}

	@Override
	public void visit(Shr node) {
		updateTarVal(node, TargetValue::shr);
	}

	@Override
	public void visit(Shrs node) {
		updateTarVal(node, TargetValue::shrs);
	}

	@Override
	public void visit(Size node) {
		updateTarVal(node, new TargetValue(node.getType().getSize(), Mode.getLu()));
	}

	@Override
	public void visit(Sub node) {
		updateTarVal(node, TargetValue::sub);
	}

	private void updateTarVal(Binop node, BinaryOperator<TargetValue> op) {
		updateTarVal(node, combineBinOp(node.getLeft(), node.getRight(), op));
	}

	private boolean updateTarVal(Node node, TargetValue newVal) {
		if (!newVal.equals(constants.get(node))) {
			for (BackEdges.Edge out : BackEdges.getOuts(node)) {
				// FIXME: Pls not on the queue
				if (!worklist.contains(out.node)) {
					worklist.addFirst(out.node);
				}
			}
			TargetValue convertedTarVal = newVal;
			if (newVal.isConstant() && node.getMode().isInt()) {
				convertedTarVal = newVal.convertTo(node.getMode());
			}
			constants.put(node, convertedTarVal);
			return true;
		}
		return false;
	}

	@SuppressWarnings("NonAsciiCharacters")
	private TargetValue φΚοµβαιν(TargetValue α, TargetValue β) {
		if (α.equals(TargetValue.getUnknown())) {
			return β;
		}
		if (β.equals(TargetValue.getUnknown())) {
			return α;
		}
		if (α.equals(β)) {
			return α;
		}
		return TargetValue.getBad();
	}

	private TargetValue combineBinOp(Node first, Node second, BinaryOperator<TargetValue> op) {
		return combineBinOp(tarValOf(first), tarValOf(second), op);
	}

	private TargetValue combineBinOp(TargetValue first, TargetValue second, BinaryOperator<TargetValue> op) {
		if (first.equals(TargetValue.getUnknown()) || second.equals(TargetValue.getUnknown())) {
			return TargetValue.getUnknown();
		}
		if (first.equals(TargetValue.getBad()) || second.equals(TargetValue.getBad())) {
			return TargetValue.getBad();
		}
		return op.apply(first, second);
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	private TargetValue tarValOf(Node node) {
		return constants.computeIfAbsent(node, ignore -> {
			// Welcome to Whack-A-Mole! Compilerpraktkum Edition 2021 (Corona Release)
			// Highscores:
			//    Chris   : 4
			//    Istannen: 4
			// Please enter your score: __

			return switch (node) {
				case Add ignored -> TargetValue.getUnknown();
				case Address ignored -> TargetValue.getUnknown();
				case Align ignored -> TargetValue.getUnknown();
				case And ignored -> TargetValue.getUnknown();
				case Bad ignored -> TargetValue.getBad();
				case Bitcast ignored -> TargetValue.getUnknown();
				case Call ignored -> TargetValue.getBad();
				case Cmp ignored -> TargetValue.getUnknown();
				case Cond ignored -> TargetValue.getUnknown();
				case Confirm ignored -> TargetValue.getUnknown();
				case Const ignored -> TargetValue.getUnknown();
				case Conv ignored -> TargetValue.getUnknown();
				case Div ignored -> TargetValue.getUnknown();
				case Dummy ignored -> TargetValue.getUnknown();
				case Eor ignored -> TargetValue.getUnknown();
				case Id id -> tarValOf(id.getPred());
				case Jmp ignored -> TargetValue.getUnknown();
				case Load ignored -> TargetValue.getBad();
				case Member ignored -> TargetValue.getBad();
				case Minus ignored -> TargetValue.getUnknown();
				case Mod ignored -> TargetValue.getUnknown();
				case Mul ignored -> TargetValue.getUnknown();
				case Mulh ignored -> TargetValue.getUnknown();
				case NoMem ignored -> TargetValue.getUnknown();
				case Not ignored -> TargetValue.getUnknown();
				case Offset ignored -> TargetValue.getUnknown();
				case Or ignored -> TargetValue.getUnknown();
				case Phi ignored -> TargetValue.getUnknown();
				case Proj ignored -> TargetValue.getUnknown();
				case Shl ignored -> TargetValue.getUnknown();
				case Shr ignored -> TargetValue.getUnknown();
				case Shrs ignored -> TargetValue.getUnknown();
				case Size ignored -> TargetValue.getUnknown();
				case Start ignored -> TargetValue.getBad();
				case Store ignored -> TargetValue.getBad();
				case Sub ignored -> TargetValue.getUnknown();
				case Unknown ignored -> TargetValue.getUnknown();
				case Mux ignored -> TargetValue.getUnknown();
				// if you reached this, your graph is broken, please enter your total time spend debugging the problem
				// Chris: 5 hours
				default -> throw new InternalCompilerException(
					"Unknown node type, can not determine constant folding default value");
			};
		});
	}

	private static String debugTarVal(TargetValue targetValue) {
		if (targetValue.equals(TargetValue.getUnknown())) {
			return "bottom";
		}
		if (targetValue.equals(TargetValue.getBad())) {
			return "top";
		}
		return targetValue.toString();
	}
}
