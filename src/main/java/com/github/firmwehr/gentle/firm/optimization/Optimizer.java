package com.github.firmwehr.gentle.firm.optimization;


import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import firm.Graph;
import firm.Program;

import java.util.ArrayList;
import java.util.List;

public class Optimizer {
	private final List<GraphOptimizationStep<Graph>> graphOptimizationSteps;
	private final List<GraphOptimizationStep<CallGraph>> callGraphOptimizationSteps;

	private Optimizer(
		List<GraphOptimizationStep<Graph>> graphOptimizationSteps,
		List<GraphOptimizationStep<CallGraph>> callGraphOptimizationSteps
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
				for (GraphOptimizationStep<Graph> step : this.graphOptimizationSteps) {
					changed |= step.optimize(graph);
				}
			} while (changed);
		}
		// TODO this should probably be repeated too, and also repeated in combination with the other steps
		CallGraph callGraph = CallGraph.create(Program.getGraphs());
		for (GraphOptimizationStep<CallGraph> step : this.callGraphOptimizationSteps) {
			step.optimize(callGraph);
		}
	}


	public static class Builder {
		private final List<GraphOptimizationStep<Graph>> graphOptimizationSteps = new ArrayList<>();
		private final List<GraphOptimizationStep<CallGraph>> callGraphOptimizationSteps = new ArrayList<>();

		public Builder addGraphStep(GraphOptimizationStep<Graph> step) {
			this.graphOptimizationSteps.add(step);
			return this;
		}

		public Builder addCallGraphStep(GraphOptimizationStep<CallGraph> step) {
			this.callGraphOptimizationSteps.add(step);
			return this;
		}

		public Optimizer build() {
			return new Optimizer(this.graphOptimizationSteps, this.callGraphOptimizationSteps);
		}
	}
}
