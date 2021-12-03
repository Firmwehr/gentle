package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import firm.BackEdges;
import firm.Dump;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.BinaryOperator;
import java.util.stream.StreamSupport;

public class ConstantFolding extends NodeVisitor.Default {
	private final Graph graph;

	private final Map<Node, TargetValue> constants = new HashMap<>();
	private final Deque<Node> worklist = new ArrayDeque<>();
	private boolean hasChanged;

	public ConstantFolding(Graph graph) {
		this.graph = graph;
	}

	public static void optimize() {
		for (Graph graph : Program.getGraphs()) {
			while (true) {
				// Needs to be done in each iteration
				BackEdges.enable(graph);
				ConstantFolding folding = new ConstantFolding(graph);
				folding.fold();
				System.out.println(graph);
				binding_irgopt.remove_bads(graph.ptr);
				binding_irgopt.remove_unreachable_code(graph.ptr);
				System.out.println("Done");

				// This should be done *after* we have removed unreachable code and bads to ensure it doesn't get
				// confused with bad preds
				folding.optimizeBlockChains();

				// Pls disable anyways
				BackEdges.disable(graph);
				if (!folding.hasChanged) {
					break;
				}
			}
			Dump.dumpGraph(graph, "cf");
		}
	}

	private void fold() {
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

		var list = new ArrayList<>(constants.entrySet());
		Collections.shuffle(list);
		for (var entry : list) {
			TargetValue tarVal = entry.getValue();
			Node node = entry.getKey();
			if (!tarVal.isConstant()) {
				continue;
			}

			if (tarVal.getMode().isInt() && node.getOpCode() != binding_irnode.ir_opcode.iro_Const) {
				Graph.exchange(node, graph.newConst(tarVal));
			} else if (node.getOpCode() == binding_irnode.ir_opcode.iro_Cond) {
				hasChanged = true;

				List<Node> nodes = StreamSupport.stream(BackEdges.getOuts(node).spliterator(), false)
					.sorted(Comparator.comparingInt(edge -> edge.pos))
					.map(edge -> edge.node)
					.toList();

				if (nodes.size() != 2) {
					throw new InternalCompilerException("Expected two nodes for Cond " + node + ", got " + nodes);
				}

				Dump.dumpGraph(graph, "before-foo");

				Node trueProj = ((Proj) nodes.get(0)).getNum() == Cond.pnTrue ? nodes.get(0) : nodes.get(1);
				Node falseProj = ((Proj) nodes.get(0)).getNum() == Cond.pnFalse ? nodes.get(0) : nodes.get(1);

				BackEdges.Edge trueEdge = BackEdges.getOuts(trueProj).iterator().next();
				Block trueBlock = (Block) trueEdge.node;
				BackEdges.Edge falseEdge = BackEdges.getOuts(falseProj).iterator().next();
				Block falseBlock = (Block) falseEdge.node;
				if (tarVal.isOne()) {
					trueBlock.setPred(trueEdge.pos, graph.newJmp(node.getBlock()));
					falseBlock.setPred(falseEdge.pos, graph.newBad(Mode.getX()));
				} else if (tarVal.isNull()) {
					falseBlock.setPred(falseEdge.pos, graph.newJmp(node.getBlock()));
					trueBlock.setPred(trueEdge.pos, graph.newBad(Mode.getX()));
				}

				graph.keepAlive(node.getBlock());
			}
		}
		Dump.dumpGraph(graph, "foo");
	}

	/**
	 * Merges blocks with their predecessor if they have a single input and the pred has an unconditional jump to them:
	 *
	 * <pre>
	 *    ◄────┐  ▲  ┌───►
	 *         │  │  │
	 *         │  │  │
	 *       ┌─┴──┴──┴─┐
	 *       │         │
	 *       │ ┌─────┐ │                     ◄────┐  ▲  ┌───►
	 *       │ │ JMP │ │    ─────┐                │  │  │
	 *       │ └──▲──┘ │         │                │  │  │
	 *       │    │    │         │              ┌─┴──┴──┴─┐
	 *       └────┼────┘         │              │         │
	 *            │              │              │         │
	 *            │              └───────►      │         │
	 *       ┌────┴────┐                        │         │
	 *       │         │                        │         │
	 *       │         │                        └─────────┘
	 *       │         │
	 *       │         │
	 *       │         │
	 *       └─────────┘
	 * </pre>
	 */
	private void optimizeBlockChains() {
		// We can not use walkBlocks as we exchange blocks while considering them, so we need to roll that ourselves
		graph.incBlockVisited();

		//		Dump.dumpGraph(graph, "eat-in");

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
		Dump.dumpGraph(graph, "eat-out");
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

		Graph.exchange(block, pred.getBlock());
		hasChanged = true;
		Dump.dumpGraph(graph, "eat");
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
		updateTarVal(node, combineBinOp(node.getLeft(), node.getRight(), TargetValue::div));
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
		updateTarVal(node, combineBinOp(node.getLeft(), node.getRight(), TargetValue::mod));
	}

	@Override
	public void visit(Mul node) {
		updateTarVal(node, TargetValue::mul);
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
	}

	private void updateTarVal(Binop node, BinaryOperator<TargetValue> op) {
		updateTarVal(node, combineBinOp(node.getLeft(), node.getRight(), op));
	}

	private void updateTarVal(Node node, TargetValue newVal) {
		if (!newVal.equals(constants.get(node))) {
			for (BackEdges.Edge out : BackEdges.getOuts(node)) {
				// FIXME: Pls not on the queue
				if (!worklist.contains(out.node)) {
					worklist.addFirst(out.node);
				}
			}
			constants.put(node, newVal);
		}
	}

	@SuppressWarnings({"NonAsciiCharacters", "SpellCheckingInspection"})
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
}
