package com.github.firmwehr.gentle.firm.optimization;

import firm.BackEdges;
import firm.Dump;
import firm.Graph;
import firm.Mode;
import firm.Program;
import firm.Relation;
import firm.TargetValue;
import firm.nodes.Add;
import firm.nodes.And;
import firm.nodes.Bad;
import firm.nodes.Binop;
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
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.StreamSupport;

public class ConstantFolding extends NodeVisitor.Default {
	private final Graph graph;

	private final Map<Node, TargetValue> constants = new HashMap<>();
	private final Deque<Node> worklist = new ArrayDeque<>();

	public ConstantFolding(Graph graph) {
		this.graph = graph;
	}

	public static void optimize() {
		for (Graph graph : Program.getGraphs()) {
			BackEdges.enable(graph);
			new ConstantFolding(graph).fold();
			BackEdges.disable(graph);
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
			if (!tarVal.isConstant()) {
				continue;
			}

			if (tarVal.getMode().isInt()) {
				Graph.exchange(entry.getKey(), graph.newConst(tarVal));
			} else if (tarVal.getMode().equals(Mode.getT())) {

			} else if (tarVal.getMode().equals(Mode.getF())) {

			}
		}
		Dump.dumpGraph(graph, "foo");
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
		updateTarVal(node, tarValOf(node.getOp()).neg());
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
		updateTarVal(node, tarValOf(node.getOp()).not());
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
