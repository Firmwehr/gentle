package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.util.GraphDumper;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;
import firm.nodes.Start;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class UnusedParameterOptimization {

	private static final int CALL_ARGUMENT_OFFSET = Start.pnTArgs; // [0: Proj M, 1: Address, 2...: Arguments...]
	private final CallGraph graph;
	private final Map<Graph, BitSet> usages;

	public UnusedParameterOptimization(CallGraph graph) {
		this.graph = graph;
		this.usages = new HashMap<>();
	}

	public static GraphOptimizationStep<CallGraph> unusedParameterOptimization() {
		return GraphOptimizationStep.<CallGraph>builder().withDescription("").build();
	}

	public void optimize() {
		this.graph.walkPostorder(g -> {
			BackEdges.enable(g);
			// replace all arguments in calls in this graph if they aren't used in their graph
			// before checking the parameter usage of this graph to avoid unneeded forwarding
			replaceUnusedForCalls(g);
			GraphDumper.dumpGraph(g, "replace-unused");
			BitSet used = new BitSet();
			for (BackEdges.Edge startEdge : BackEdges.getOuts(g.getStart())) { // Proj M and Proj T
				if (startEdge.node instanceof Proj argsProj && argsProj.getNum() == Start.pnTArgs) {
					for (BackEdges.Edge edge : BackEdges.getOuts(argsProj)) { // Proj <Type> Arg <Index>
						if (edge.node instanceof Proj proj) {
							used.set(proj.getNum());
						}
					}
				}
			}
			this.usages.put(g, used);

			BackEdges.disable(g);
		});
	}

	private void replaceUnusedForCalls(Graph graph) {
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				Address address = (Address) node.getPred(1);
				if (address.getEntity().getGraph() == null) {
					return;
				}
				BitSet usageForCallee = UnusedParameterOptimization.this.usages.get(address.getEntity().getGraph());
				// TODO recursion makes this harder - but it should be possible to make it work
				// if bit set is null, the method wasn't scanned before => recursion (or a wrong order)
				if (usageForCallee == null) {
					return;
				}
				for (int i = CALL_ARGUMENT_OFFSET; i < node.getPredCount(); i++) {
					if (!usageForCallee.get(i - CALL_ARGUMENT_OFFSET)) {
						Graph.exchange(node.getPred(i), graph.newUnknown(node.getPred(i).getMode()));
					}
				}
			}
		});
	}
}
