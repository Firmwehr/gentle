package com.github.firmwehr.gentle.firm.optimization;


import firm.Graph;
import firm.Program;

import java.util.ArrayList;
import java.util.List;

public class Optimizer {
	private final List<GraphOptimizationStep> graphOptimizationSteps;

	private Optimizer(List<GraphOptimizationStep> graphOptimizationSteps) {
		this.graphOptimizationSteps = List.copyOf(graphOptimizationSteps);
	}

	public static Builder builder() {
		return new Builder();
	}

	public void optimize() {
		for (Graph graph : Program.getGraphs()) {
			boolean changed;
			do {
				changed = false;
				for (GraphOptimizationStep step : this.graphOptimizationSteps) {
					changed |= step.optimize(graph);
				}
			} while (changed);
		}
	}


	public static class Builder {
		private final List<GraphOptimizationStep> graphOptimizationSteps = new ArrayList<>();

		public Builder addStep(GraphOptimizationStep step) {
			this.graphOptimizationSteps.add(step);
			return this;
		}

		public Optimizer build() {
			return new Optimizer(this.graphOptimizationSteps);
		}
	}
}
