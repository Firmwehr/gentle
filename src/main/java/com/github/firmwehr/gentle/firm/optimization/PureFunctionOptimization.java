package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import firm.Graph;

import java.util.Set;

public class PureFunctionOptimization {
	private final CallGraph callGraph;

	public PureFunctionOptimization(CallGraph callGraph) {
		this.callGraph = callGraph;
	}

	public static GraphOptimizationStep<CallGraph, Set<Graph>> pureFunctionOptimization() {
		return GraphOptimizationStep.<CallGraph, Set<Graph>>builder()
			.withDescription("PureFunctionOptimization")
			.withOptimizationFunction(callGraph -> new PureFunctionOptimization(callGraph).optimize())
			.build();
	}

	private Set<Graph> optimize() {
		return Set.of(); // TODO
	}
}
