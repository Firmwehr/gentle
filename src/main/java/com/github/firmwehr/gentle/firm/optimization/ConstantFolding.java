package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.Program;
import firm.Relation;
import firm.TargetValue;
import firm.bindings.binding_irgopt;
import firm.bindings.binding_irnode;
import firm.nodes.Add;
import firm.nodes.And;
import firm.nodes.Bad;
import firm.nodes.Binop;
import firm.nodes.Block;
import firm.nodes.Cmp;
import firm.nodes.Cond;
import firm.nodes.Const;
import firm.nodes.Conv;
import firm.nodes.Div;
import firm.nodes.Id;
import firm.nodes.Member;
import firm.nodes.Minus;
import firm.nodes.Mod;
import firm.nodes.Mul;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Not;
import firm.nodes.Or;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Sel;
import firm.nodes.Shl;
import firm.nodes.Shr;
import firm.nodes.Shrs;
import firm.nodes.Size;
import firm.nodes.Sub;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.StreamSupport;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class ConstantFolding extends NodeVisitor.Default {
	private static final Logger LOGGER = new Logger(ConstantFolding.class);

	private final Graph graph;
	// stores associated lattice element for each visited note during fixed point iteration
	private final Map<Node, TargetValue> constants;
	private final Deque<Node> worklist;
	private final Map<Node, Node> replacements;

	// keeps track of chages in each iteration until fixed point is reached
	private boolean hasChangedInCurrentIteration;

	public ConstantFolding(Graph graph) {
		this.graph = graph;
		this.worklist = new ArrayDeque<>();
		this.constants = new HashMap<>();
		this.replacements = new HashMap<>();
	}

	public static void optimize() {
		LOGGER.info("Started");
		for (Graph graph : Program.getGraphs()) {
			LOGGER.info("Running constant folding for %s", graph);

			while (true) {
				// Needs to be done in each iteration apparently?
				BackEdges.enable(graph);

				ConstantFolding folding = new ConstantFolding(graph);
				folding.fold();
				binding_irgopt.remove_bads(graph.ptr);
				binding_irgopt.remove_unreachable_code(graph.ptr);

				// This should be done *after* we have removed unreachable code and bads to ensure it doesn't get
				// confused with bad preds
				folding.optimizeBlockChains();

				// testing has shown that back edges get disabled anyway for some reason, but we don't like problems
				BackEdges.disable(graph);

				if (!folding.hasChangedInCurrentIteration) {
					break;
				}
			}
			dumpGraph(graph, "cf");
		}
		LOGGER.info("Finished");
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

		// FIXME: Extract replacements to own class/pass: https://github.com/Firmwehr/gentle/issues/103
		if (!replacements.isEmpty()) {
			LOGGER.debugHeader("Replacing");
			for (var entry : replacements.entrySet()) {
				LOGGER.debug("Replacing   %-25s with %-25s", entry.getKey(), entry.getValue());
				Graph.exchange(entry.getKey(), entry.getValue());

				// We might be registered as the replacement for another node
				// (e.g. because we were on the left side of 'x + 0').
				// In that case we need to register *our replacement* as the replacement.
				for (var otherEntry : replacements.entrySet()) {
					if (otherEntry.getValue().equals(entry.getKey())) {
						otherEntry.setValue(entry.getValue());
					}
				}
			}
		}

		LOGGER.debugHeader("Folding");

		boolean hasChangedInAnyIteration = this.hasChangedInCurrentIteration;
		for (var entry : constants.entrySet()) {
			this.hasChangedInCurrentIteration = false;

			TargetValue tarVal = entry.getValue();
			Node node = entry.getKey();
			if (!tarVal.isConstant()) {
				continue;
			}
			LOGGER.debug("Considering %-25s with tarval %s", node, debugTarVal(tarVal));

			// Replacing const with const is meaningless and causes infinite iterations as hasChanged is always true
			if (tarVal.getMode().isInt() && node.getOpCode() != binding_irnode.ir_opcode.iro_Const) {
				LOGGER.debug("Replacing   %-25s with tarval %s", node, debugTarVal(tarVal));

				// Add -> Const
				if (node instanceof Div div) {
					replace(div, div.getMem(), graph.newConst(tarVal), Graph::exchange);
				} else if (node instanceof Mod mod) {
					replace(mod, mod.getMem(), graph.newConst(tarVal), Graph::exchange);
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

		List<Node> nodes =
			StreamSupport.stream(BackEdges.getOuts(node).spliterator(), false).map(edge -> edge.node).toList();

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
		if (tarValOf(node.getRight()).isNull()) {
			replacements.put(node, node.getLeft());
		}
		if (tarValOf(node.getLeft()).isNull()) {
			replacements.put(node, node.getRight());
		}
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

		// Some algebraic identities
		TargetValue rightVal = tarValOf(node.getRight());

		// a / 1 => a
		if (rightVal.isOne()) {
			replace(node, node.getMem(), node.getLeft(), replacements::put);
		}
		// a / -1 => -a
		if (rightVal.isConstant() && rightVal.abs().isOne() && rightVal.isNegative()) {
			replace(node, node.getMem(), graph.newMinus(node.getBlock(), node.getLeft()), replacements::put);
		}
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
	 * @param replacementAction the action implementing the replacement (e.g. {@link Graph#exchange(Node, Node)} or
	 * 	adding it to the replacement map. We can not perform structural changes during constant folding iterations, as
	 * 	that would invalidate parts of the current analysis. Therefore, it might need to be deferred.
	 */
	private void replace(Node node, Node previousMemory, Node replacement, BiConsumer<Node, Node> replacementAction) {
		for (BackEdges.Edge out : BackEdges.getOuts(node)) {
			if (out.node.getMode().equals(Mode.getM())) {
				replacementAction.accept(out.node, previousMemory);
			} else {
				replacementAction.accept(out.node, replacement);
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
		TargetValue leftTarVal = tarValOf(node.getLeft());
		TargetValue rightTarVal = tarValOf(node.getRight());

		// TODO: move to separate phase
		// some algebraic identities
		// 1 * a => a
		if (leftTarVal.isOne()) {
			replacements.put(node, node.getRight());
		}
		// a * 1 => a
		if (rightTarVal.isOne()) {
			replacements.put(node, node.getLeft());
		}
		// 0 * a => 0 && a * 0 => 0
		if (leftTarVal.isNull() || rightTarVal.isNull()) {
			replacements.put(node, graph.newConst(0, node.getLeft().getMode()));
		}
		// a * -1 => -a
		if (rightTarVal.isConstant() && rightTarVal.isNegative() && rightTarVal.abs().isOne()) {
			replacements.put(node, graph.newMinus(node.getBlock(), node.getLeft()));
		}
		// -1 * a => -a
		if (leftTarVal.isConstant() && leftTarVal.isNegative() && leftTarVal.abs().isOne()) {
			replacements.put(node, graph.newMinus(node.getBlock(), node.getRight()));
		}
		// TODO (maybe): 2 * a => a << 2
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
		TargetValue targetValue = StreamSupport.stream(node.getPreds().spliterator(), false)
			.map(this::tarValOf)
			.reduce(this::φΚοµβαιν)
			.orElseThrow();

		updateTarVal(node, targetValue);
	}

	@Override
	public void visit(Proj node) {
		if (node.getMode().isInt()) {
			updateTarVal(node, tarValOf(node.getPred()));
		}
	}

	@Override
	public void visit(Return node) {
	}

	@Override
	public void visit(Sel node) {
		// TODO: Fold unchanged array values?
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

		TargetValue rightVal = tarValOf(node.getRight());
		TargetValue leftVal = tarValOf(node.getLeft());

		if (rightVal.isNull()) {
			replacements.put(node, node.getLeft());
		}
		if (leftVal.isNull()) {
			replacements.put(node, graph.newMinus(node.getBlock(), node.getRight()));
		}
		if (leftVal.isConstant() && leftVal.equals(rightVal)) {
			replacements.put(node, graph.newConst(0, node.getLeft().getMode()));
		}
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

	private TargetValue tarValOf(Node node) {
		return constants.getOrDefault(node, TargetValue.getUnknown());
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
