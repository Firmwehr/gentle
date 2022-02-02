package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.CmpWithConstPattern;
import com.github.firmwehr.fiascii.generated.CmpWithLoadPattern;
import com.github.firmwehr.fiascii.generated.CommutativeWithConstPattern;
import com.github.firmwehr.fiascii.generated.CommutativeWithLoadPattern;
import com.github.firmwehr.gentle.util.GraphDumper;
import firm.Graph;
import firm.nodes.Add;
import firm.nodes.And;
import firm.nodes.Const;
import firm.nodes.Eor;
import firm.nodes.Load;
import firm.nodes.Mul;
import firm.nodes.Mulh;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;

import java.util.Optional;
import java.util.Set;

public class ReorderInputsOptimization extends NodeVisitor.Default {

	private static final OptimizationList OPTIMIZATIONS = OptimizationList.builder()
		.addStep(ReorderInputsOptimization::commutativeWithConst,
			(match, graph, block) -> swap(match.op(), match.left(), match.right()))
		.addStep(ReorderInputsOptimization::commutativeWithLoad,
			(match, graph, block) -> swap(match.op(), match.leftP(), match.right()))
		.addStep(ReorderInputsOptimization::cmpWithConst, (match, graph, block) -> {
			match.cmp().setRelation(match.cmp().getRelation().inversed());
			return swap(match.cmp(), match.left(), match.right());
		})
		.addStep(ReorderInputsOptimization::cmpWithLoad, (match, graph, block) -> {
			match.cmp().setRelation(match.cmp().getRelation().inversed());
			return swap(match.cmp(), match.leftP(), match.right());
		})
		.build();

	private static boolean swap(Node op, Node left, Node right) {
		op.setPred(0, right); // 0 = left
		op.setPred(1, left);  // 1 = right
		return true;
	}

	private static final Set<Class<? extends Node>> COMMUTATIVE_OPS =
		Set.of(Add.class, And.class, Eor.class, Mul.class, Mulh.class);
	private final Graph graph;
	private boolean changed;

	private ReorderInputsOptimization(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> reorderInputs() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("ReorderInputsOptimization")
			.withOptimizationFunction(graph -> new ReorderInputsOptimization(graph).optimize())
			.build();
	}

	private boolean optimize() {
		graph.walk(this);
		if (changed) {
			GraphDumper.dumpGraph(graph, "reorder-inputs");
		}
		return changed;
	}

	@Override
	public void defaultVisit(Node n) {
		changed |= OPTIMIZATIONS.optimize(n, n.getGraph(), n.getBlock());
	}

	@FiAscii("""
		┌────────────┐  ┌─────────┐
		│left: Const │  │right: * │
		└──────┬─────┘  └───┬─────┘
		       │            │
		       └──┐      ┌──┘
		          │      │
		        ┌─▼──────▼─┐
		        │ cmp: Cmp │
		        └──────────┘""")
	public static Optional<CmpWithConstPattern.Match> cmpWithConst(Node node) {
		return CmpWithConstPattern.match(node)
			// if right is a constant too, we'd swap infinitely without any benefit
			.filter(match -> !(match.right() instanceof Const));
	}

	@FiAscii("""
		 ┌────────────┐
		 │ left: Load │
		 └─────┬──────┘
		       │
		┌──────▼─────┐  ┌─────────┐
		│leftP: Proj │  │right: * │
		└──────┬─────┘  └───┬─────┘
		       │            │
		       └──┐      ┌──┘
		          │      │
		        ┌─▼──────▼─┐
		        │ cmp: Cmp │
		        └──────────┘""")
	public static Optional<CmpWithLoadPattern.Match> cmpWithLoad(Node node) {
		return CmpWithLoadPattern.match(node)
			// if right is a constant or a load too, we'd swap infinitely without any benefit
			.filter(match -> !(match.right() instanceof Const ||
				(match.right() instanceof Proj proj && proj.getPred() instanceof Load)));
	}

	@FiAscii("""
		┌────────────┐  ┌─────────┐
		│left: Const │  │right: * │
		└──────┬─────┘  └───┬─────┘
		       │            │
		       └────┐   ┌───┘
		            │   │
		          ┌─▼───▼─┐
		          │ op: * │
		          └───────┘""")
	public static Optional<CommutativeWithConstPattern.Match> commutativeWithConst(Node node) {
		// shortcut to avoid lots of unnecessary match calls
		if (!ReorderInputsOptimization.COMMUTATIVE_OPS.contains(node.getClass())) {
			return Optional.empty();
		}
		return CommutativeWithConstPattern.match(node)
			// if right is a constant too, we'd swap infinitely without any benefit
			.filter(match -> !(match.right() instanceof Const));
	}

	@FiAscii("""
		 ┌────────────┐
		 │ left: Load │
		 └─────┬──────┘
		       │
		┌──────▼─────┐  ┌─────────┐
		│leftP: Proj │  │right: * │
		└──────┬─────┘  └───┬─────┘
		       │            │
		       └────┐   ┌───┘
		            │   │
		          ┌─▼───▼─┐
		          │ op: * │
		          └───────┘""")
	public static Optional<CommutativeWithLoadPattern.Match> commutativeWithLoad(Node node) {
		// shortcut to avoid lots of unnecessary match calls
		if (!ReorderInputsOptimization.COMMUTATIVE_OPS.contains(node.getClass())) {
			return Optional.empty();
		}
		return CommutativeWithLoadPattern.match(node)
			// if right is a constant or a load too, we'd swap infinitely without any benefit
			.filter(match -> !(match.right() instanceof Const ||
				(match.right() instanceof Proj proj && proj.getPred() instanceof Load)));
	}
}
