package com.github.firmwehr.gentle.firm.optimization;


import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import firm.Graph;
import firm.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Optimizer {
	private final List<GraphOptimizationStep<Graph, Boolean>> graphOptimizationSteps;
	private final List<GraphOptimizationStep<CallGraph, Set<Graph>>> callGraphOptimizationSteps;

	private Optimizer(
		List<GraphOptimizationStep<Graph, Boolean>> graphOptimizationSteps,
		List<GraphOptimizationStep<CallGraph, Set<Graph>>> callGraphOptimizationSteps
	) {
		this.graphOptimizationSteps = List.copyOf(graphOptimizationSteps);
		this.callGraphOptimizationSteps = List.copyOf(callGraphOptimizationSteps);
	}

	public static Builder builder() {
		return new Builder();
	}

	public void optimize() {
		for (Graph graph : Program.getGraphs()) {
			boolean changed;
			do {
				changed = false;
				for (GraphOptimizationStep<Graph, Boolean> step : this.graphOptimizationSteps) {
					changed |= step.optimize(graph);
				}
			} while (changed);
		}
		// TODO this should probably be repeated too, and also repeated in combination with the other steps
		CallGraph callGraph = CallGraph.create(Program.getGraphs());
		for (GraphOptimizationStep<CallGraph, Set<Graph>> step : this.callGraphOptimizationSteps) {
			callGraph = callGraph.updated(step.optimize(callGraph));
		}
	}


	public static class Builder {
		private final List<GraphOptimizationStep<Graph, Boolean>> graphOptimizationSteps = new ArrayList<>();
		private final List<GraphOptimizationStep<CallGraph, Set<Graph>>> callGraphOptimizationSteps =
			new ArrayList<>();

		public Builder addGraphStep(GraphOptimizationStep<Graph, Boolean> step) {
			this.graphOptimizationSteps.add(step);
			return this;
		}

		public Builder addCallGraphStep(GraphOptimizationStep<CallGraph, Set<Graph>> step) {
			this.callGraphOptimizationSteps.add(step);
			return this;
		}

		public Optimizer build() {
			return new Optimizer(this.graphOptimizationSteps, this.callGraphOptimizationSteps);
		}
	}
}
