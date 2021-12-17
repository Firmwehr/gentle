package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.output.Logger;
import com.google.common.base.Preconditions;
import firm.Graph;

public final class GraphOptimizationStep {
	private static final Logger LOGGER = new Logger(GraphOptimizationStep.class);
	private final String description;
	private final OptimizationFunction optimizationFunction;

	private GraphOptimizationStep(String description, OptimizationFunction optimizationFunction) {
		this.description = description;
		this.optimizationFunction = optimizationFunction;
	}


	public boolean optimize(Graph graph) {
		LOGGER.info("Running %s for %s", this.description, graph);
		return this.optimizationFunction.optimize(graph);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String description;
		private OptimizationFunction optimizationFunction;

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder withOptimizationFunction(OptimizationFunction optimizationFunction) {
			this.optimizationFunction = optimizationFunction;
			return this;
		}

		public GraphOptimizationStep build() {
			Preconditions.checkState(this.description != null, "Description must be set");
			Preconditions.checkState(this.optimizationFunction != null, "Optimization Function must be set");
			return new GraphOptimizationStep(this.description, this.optimizationFunction);
		}
	}

	@FunctionalInterface
	public interface OptimizationFunction {

		boolean optimize(Graph graph);
	}
}
