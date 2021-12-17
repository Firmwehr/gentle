package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.output.Logger;
import com.google.common.base.Preconditions;
import firm.Graph;

import java.util.function.Predicate;

public final class GraphOptimizationStep implements Predicate<Graph> {
	private static final Logger LOGGER = new Logger(GraphOptimizationStep.class);
	private String description;
	private Predicate<Graph> optimizationFunction;

	private GraphOptimizationStep(String description, Predicate<Graph> optimizationFunction) {
		this.description = description;
		this.optimizationFunction = optimizationFunction;
	}


	public boolean optimize(Graph graph) {
		LOGGER.info("Running %s for %s", this.description, graph);
		return this.optimizationFunction.test(graph);
	}

	@Override
	public boolean test(Graph graph) {
		return optimize(graph);
	}

	public static Builder builder() {
		return new Builder();
	}

	static class Builder {
		private String description;
		private Predicate<Graph> optimizationFunction;

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder withOptimizationFunction(Predicate<Graph> optimizationFunction) {
			this.optimizationFunction = optimizationFunction;
			return this;
		}

		public GraphOptimizationStep build() {
			Preconditions.checkState(this.description != null, "Description must be set");
			Preconditions.checkState(this.optimizationFunction != null, "Optimization Function must be set");
			return new GraphOptimizationStep(this.description, this.optimizationFunction);
		}
	}
}
