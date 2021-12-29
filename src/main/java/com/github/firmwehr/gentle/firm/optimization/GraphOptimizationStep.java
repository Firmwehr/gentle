package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.output.Logger;
import com.google.common.base.Preconditions;
import firm.Graph;

public final class GraphOptimizationStep<T> {
	private static final Logger LOGGER = new Logger(GraphOptimizationStep.class);
	private final String description;
	private final OptimizationFunction<T> optimizationFunction;

	private GraphOptimizationStep(String description, OptimizationFunction<T> optimizationFunction) {
		this.description = description;
		this.optimizationFunction = optimizationFunction;
	}


	public boolean optimize(T t) {
		LOGGER.info("Running %s for %s", this.description, t);
		return this.optimizationFunction.optimize(t);
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static class Builder<T> {
		private String description;
		private OptimizationFunction<T> optimizationFunction;

		public Builder<T> withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder<T> withOptimizationFunction(OptimizationFunction<T> optimizationFunction) {
			this.optimizationFunction = optimizationFunction;
			return this;
		}

		public GraphOptimizationStep<T> build() {
			Preconditions.checkState(this.description != null, "Description must be set");
			Preconditions.checkState(this.optimizationFunction != null, "Optimization Function must be set");
			return new GraphOptimizationStep<>(this.description, this.optimizationFunction);
		}
	}

	@FunctionalInterface
	public interface OptimizationFunction<T> {

		boolean optimize(T t);
	}
}
