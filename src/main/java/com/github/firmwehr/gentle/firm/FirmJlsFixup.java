package com.github.firmwehr.gentle.firm;

import com.github.firmwehr.gentle.util.GraphDumper;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.Const;
import firm.nodes.Div;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Transforms a firm graph to avoid incompatibilities caused by behavioral
 * differences between x86 instructions and the (M)JLS.
 */
public final class FirmJlsFixup {

	private FirmJlsFixup() {
		throw new AssertionError();
	}

	public static void fix(Graph graph) {
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Div node) {
				if (isSafeDiv(node.getLeft(), node.getRight())) {
					return;
				}
				// expansion to 64 bit to gracefully handle MIN_INT / -1 case which is valid in Java but causes
				// floating point exception (sic) on x86
				node.setResmode(Mode.getLs());
				insertConv(graph, node::getLeft, node::setLeft, Mode.getLs());
				insertConv(graph, node::getRight, node::setRight, Mode.getLs());
				findResProj(node).ifPresent(proj -> rewriteResProj(graph, proj));
			}

			@Override
			public void visit(Mod node) {
				if (isSafeDiv(node.getLeft(), node.getRight())) {
					return;
				}
				// expansion to 64 bit to gracefully handle MIN_INT % -1 case which is valid in Java but causes
				// floating point exception (sic) on x86
				node.setResmode(Mode.getLs());
				insertConv(graph, node::getLeft, node::setLeft, Mode.getLs());
				insertConv(graph, node::getRight, node::setRight, Mode.getLs());
				findResProj(node).ifPresent(proj -> rewriteResProj(graph, proj));
			}
		});
		BackEdges.disable(graph);
		GraphDumper.dumpGraph(graph, "prepare-for-firm-be");
	}

	private static void rewriteResProj(Graph graph, Proj proj) {
		Proj newProj = (Proj) graph.newProj(proj.getPred(), Mode.getLs(), Mod.pnRes);// Mod.pnRes == Div.pnRes
		Graph.exchange(proj, newProj);
		insertConv(graph, () -> newProj, fixProj(newProj), Mode.getIs());
	}

	private static void insertConv(Graph graph, Supplier<Node> predGet, Consumer<Node> predSet, Mode mode) {
		predSet.accept(graph.newConv(predGet.get().getBlock(), predGet.get(), mode));
	}

	private static Consumer<Node> fixProj(Proj proj) {
		return conv -> {
			for (BackEdges.Edge edge : BackEdges.getOuts(proj)) {
				// our new conv already has the proj as pred, but we want to ignore it here
				if (!edge.node.equals(conv)) {
					edge.node.setPred(edge.pos, conv);
				}
			}
		};
	}

	private static Optional<Proj> findResProj(Node node) {
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			if (edge.node instanceof Proj proj && !proj.getMode().equals(Mode.getM())) {
				return Optional.of(proj);
			}
		}
		return Optional.empty();
	}

	/**
	 * The JLS requires {@code Integer.MIN_VALUE / -1} to be {@code -1} and
	 * {@code Integer.MIN_VALUE % -1} to be {@code 0}. x86 throws a Floating Point Exception
	 * if 32 bit division is called with those arguments.
	 *
	 * If either the left or the right side is a constant that is not critical in terms of
	 * causing such an issue, we can safely use the 32 bit instruction generated by firm.
	 * Otherwise, we want it to generate 64 bit instructions.
	 */
	private static boolean isSafeDiv(Node left, Node right) {
		if (left instanceof Const c && c.getTarval().asInt() != Integer.MIN_VALUE) {
			return true;
		}
		//noinspection RedundantIfStatement
		if (right instanceof Const c && c.getTarval().asInt() != -1) {
			return true;
		}
		return false;
	}
}
