package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.output.Logger;
import com.google.common.base.Preconditions;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;


public final class GraphOptimizationStep<T, R> {
	private static final Logger LOGGER = new Logger(GraphOptimizationStep.class);
	private final String description;
	private final OptimizationFunction<T, R> optimizationFunction;

	private GraphOptimizationStep(String description, OptimizationFunction<T, R> optimizationFunction) {
		this.description = description;
		this.optimizationFunction = optimizationFunction;
	}


	public R optimize(T t) {
		LOGGER.info("Running %s for %s", this.description, t);
		OptimizationEvent event = new OptimizationEvent(this.description, String.valueOf(t));
		event.begin();
		R result = this.optimizationFunction.optimize(t);
		event.result = String.valueOf(result);
		event.commit();
		return result;
	}

	public static <T, C> Builder<T, C> builder() {
		return new Builder<>();
	}

	public static class Builder<T, R> {
		private String description;
		private OptimizationFunction<T, R> optimizationFunction;

		public Builder<T, R> withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder<T, R> withOptimizationFunction(OptimizationFunction<T, R> optimizationFunction) {
			this.optimizationFunction = optimizationFunction;
			return this;
		}

		public GraphOptimizationStep<T, R> build() {
			Preconditions.checkState(this.description != null, "Description must be set");
			Preconditions.checkState(this.optimizationFunction != null, "Optimization Function must be set");
			return new GraphOptimizationStep<>(this.description, this.optimizationFunction);
		}
	}

	@FunctionalInterface
	public interface OptimizationFunction<T, R> {

		R optimize(T t);
	}

	@Label("FirmOptimization")
	@Description("An optimization by gentle on firm graphs")
	@Category("Gentle")
	private static class OptimizationEvent extends Event {
		@Label("Type")
		private final String type;

		@Label("Object")
		private final String object;

		@Label("Result")
		@Description("The result of the optimization")
		private String result;

		private OptimizationEvent(String type, String object) {
			this.type = type;
			this.object = object;
		}
	}
}
