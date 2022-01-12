package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import firm.Graph;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodInliningOptimization {
	// values carefully picked by having no clue
	private static final double CALLEE_INLINE_COST_THRESHOLD = 16d;
	private static final double CALLER_INLINE_COST_THRESHOLD = 32d;
	private static final double CALLEE_INLINE_COST_THRESHOLD_COMBINED = 40d;
	private final CallGraph callGraph;
	private boolean changed;

	public MethodInliningOptimization(CallGraph callGraph) {
		this.callGraph = callGraph;
	}

	private boolean optimize() {
		Map<Graph, Double> inlineCandidates = new HashMap<>();
		callGraph.walkPostorder(graph -> {
			double selfCost = new CostCalculator(graph).cost();
			if (selfCost > CALLER_INLINE_COST_THRESHOLD) {
				return;
			}
			Map<Call, Graph> localCandidates = findInlineCandidates(graph, inlineCandidates, selfCost);
			inlineAll(localCandidates);
			selfCost = new CostCalculator(graph).cost();
			if (selfCost < CALLEE_INLINE_COST_THRESHOLD) {
				inlineCandidates.put(graph, selfCost);
			}
		});
		return changed;
	}

	private void inlineAll(Set<Graph> candidates) {
		for (Graph c : candidates) {
			inline(c);
		}
		changed = true;
	}

	private Map<Call, Graph> findInlineCandidates(Graph graph, Map<Graph, Double> candidates, double selfCost) {
		List<InlineCandidate> result = new ArrayList<>();
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				Graph graph = ((Address) node.getPtr()).getEntity().getGraph();
				if (graph != null) {
					if (candidates.getOrDefault(graph, Double.MAX_VALUE) + selfCost < CALLEE_INLINE_COST_THRESHOLD_COMBINED) {
						result.add(new InlineCandidate(node, graph, ));
					}
				}
			}
		});
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
