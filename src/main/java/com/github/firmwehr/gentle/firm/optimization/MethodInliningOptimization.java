package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import firm.Graph;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodInliningOptimization {
	// values carefully picked by having no clue
	private static final double CALLEE_INLINE_COST_THRESHOLD = 16d;
	private static final double CALLER_INLINE_COST_THRESHOLD = 32d;
	private static final double INCREASE_THRESHOLD = 26d;
	private final CallGraph callGraph;

	private MethodInliningOptimization(CallGraph callGraph) {
		this.callGraph = callGraph;
	}

	public static GraphOptimizationStep<CallGraph, Set<Graph>> methodInlineOptimization() {
		return GraphOptimizationStep.<CallGraph, Set<Graph>>builder()
			.withDescription("MethodInlining")
			.withOptimizationFunction(callGraph -> new MethodInliningOptimization(callGraph).optimize())
			.build();
	}

	private Set<Graph> optimize() {
		Map<Graph, Double> inlineCandidates = new HashMap<>();
		Set<Graph> changed = new HashSet<>();
		callGraph.walkPostorder(graph -> {
			double selfCost = new CostCalculator(graph).cost();
			if (selfCost > CALLER_INLINE_COST_THRESHOLD) {
				return;
			}
			List<InlineCandidate> localCandidates = findInlineCandidates(graph, inlineCandidates);
			if (inlineAll(graph, localCandidates)) {
				changed.add(graph);
				selfCost = new CostCalculator(graph).cost();
				if (selfCost < CALLER_INLINE_COST_THRESHOLD) {
					inlineCandidates.put(graph, selfCost);
				}
			}
		});
		return changed;
	}

	private boolean inlineAll(Graph graph, List<InlineCandidate> candidates) {
		double approxAddedCost = 0;
		// we want to inline cheap methods first (as calling overhead is dominant there)
		candidates.sort(Comparator.comparingDouble(InlineCandidate::cost));
		for (InlineCandidate c : candidates) {
			inline(graph, c);
			approxAddedCost += c.cost();
			if (approxAddedCost > INCREASE_THRESHOLD) {
				return true;
			}
		}
		return approxAddedCost > 0; // true if anything was inlined
	}

	private void inline(Graph graph, InlineCandidate candidate) {
		Map<Node, Node> calleeToCallerCopied = new HashMap<>();
		Graph toInline = candidate.graph();
		toInline.walkPostorder(new NodeVisitor.Default() {
			@Override
			public void defaultVisit(Node old) {
				Node copy = old.copyInto(graph);
				calleeToCallerCopied.put(old, copy);
				for (int i = 0; i < old.getPredCount(); i++) {
					// set the new pred
					copy.setPred(i, calleeToCallerCopied.get(old.getPred(i)));
				}
			}
		});

	}

	private List<InlineCandidate> findInlineCandidates(Graph graph, Map<Graph, Double> candidates) {
		List<InlineCandidate> result = new ArrayList<>();
		Set<Call> calls = callGraph.callSitesIn(graph);
		for (Call call : calls) {
			Graph called = ((Address) call.getPtr()).getEntity().getGraph();
			if (called != null) { // is null for runtime calls
				double cost = candidates.getOrDefault(called, Double.MAX_VALUE);
				result.add(new InlineCandidate(call, called, cost));
			}
		}
		return result;
	}

	record InlineCandidate(Call call, Graph graph, double cost) {

	}

	// TODO
	// Should be aware of:
	// - memory
	// - loops
	// 
	static class CostCalculator extends NodeVisitor.Default {
		private final Graph graph;
		private double cost;
		private final Map<Node, Double> costMap;

		CostCalculator(Graph graph) {
			this.graph = graph;
			this.costMap = new HashMap<>();
			this.cost = Double.NaN; // lazy, with NaN as invalid value
			graph.walk(this);
		}

		public double cost() {
			if (Double.isNaN(cost)) {
				cost = costMap.values().stream().mapToDouble(Double::doubleValue).sum();
			}
			return cost;
		}

		public double costOf(Node node) {
			return costMap.getOrDefault(node, 0d); // TODO defaultValue needed?
		}

		@Override
		public void defaultVisit(Node n) {
			costMap.put(n, 1d);
		}
	}
}
