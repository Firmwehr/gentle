package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.util.GraphDumper;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Conv;
import firm.nodes.Load;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;

public class FirmGraphCleanup extends NodeVisitor.Default {

	private final Graph graph;
	private boolean changed;

	private FirmGraphCleanup(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> firmGraphCleanup() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("FirmGraphCleanup")
			.withOptimizationFunction(graph -> new FirmGraphCleanup(graph).optimize())
			.build();
	}

	private boolean optimize() {
		BackEdges.enable(graph);
		graph.walk(this);
		BackEdges.disable(graph);
		if (changed) {
			GraphDumper.dumpGraph(graph, "cleanup");
		}
		return changed;
	}

	@Override
	public void visit(Conv node) {
		// no need for Conv, modes match
		if (node.getMode().equals(node.getOp().getMode())) {
			for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
				edge.node.setPred(edge.pos, node.getOp());
			}
			changed = true;
		}
	}

	@Override
	public void visit(Phi node) {
		// remove trivial Phis
		Node current = null;
		for (Node pred : node.getPreds()) {
			if (current == null) {
				current = pred;
			} else if (!current.equals(pred)){
				return;
			}
		}
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			edge.node.setPred(edge.pos, current);
		}
		changed = true;
	}

	@Override
	public void visit(Load node) {
		// remove loads without result Proj
		if (BackEdges.getNOuts(node) == 1) {
			Node memoryProj = BackEdges.getOuts(node).iterator().next().node;
			for (BackEdges.Edge edge : BackEdges.getOuts(memoryProj)) {
				edge.node.setPred(edge.pos, node.getMem());
			}
			changed = true;
		}
	}
}
