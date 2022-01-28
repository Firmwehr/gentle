package com.github.firmwehr.gentle.firm.optimization;


import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import firm.Graph;
import firm.Program;
import firm.bindings.binding_irprog;
import firm.nodes.Address;
import firm.nodes.Call;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
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
		localOptimizations(Program.getGraphs());
		while (true) {
			Set<Graph> modified = new HashSet<>();
			Set<Graph> freed = new HashSet<>();
			while (true) {
				GlobalOptimizationResult result = globalOptimizations();
				freed.addAll(result.deleted());
				if (result.modified().isEmpty()) {
					break; // break from do while loop, no more global optimizations in this round
				}
				modified.addAll(result.modified());
			}
			// the graph should not be passed to any other optimizations after it was freed
			modified.removeAll(freed);
			if (!localOptimizations(modified)) {
				return; // no more local changes, so nothing left to optimize
			}
		}
	}

	private GlobalOptimizationResult globalOptimizations() {
		Set<Graph> modifiedCollect = new HashSet<>();
		CallGraph callGraph = CallGraph.create(Program.getGraphs());
		for (GraphOptimizationStep<CallGraph, Set<Graph>> step : this.callGraphOptimizationSteps) {
			Set<Graph> modified = step.optimize(callGraph);
			modifiedCollect.addAll(modified);
			callGraph = callGraph.updated(modified);
		}
		callGraph.debug();
		Set<Graph> freed = deleteUnused(callGraph);
		return new GlobalOptimizationResult(modifiedCollect, freed);
	}

	record GlobalOptimizationResult(
		Set<Graph> modified,
		Set<Graph> deleted
	) {
	}

	private Set<Graph> deleteUnused(CallGraph callGraph) {
		Set<Graph> used = new HashSet<>();
		Set<Graph> freed = new HashSet<>();
		Queue<Graph> workList = new ArrayDeque<>();
		Graph main = new Graph(binding_irprog.get_irp_main_irg());
		used.add(main); // main is never called but always used
		workList.add(main);
		while (!workList.isEmpty()) {
			Graph next = workList.remove();
			for (Call call : callGraph.callSitesIn(next)) {
				Graph graph = ((Address) call.getPtr()).getEntity().getGraph();
				if (graph != null && used.add(graph)) {
					workList.add(graph);
				}
			}
		}
		for (Graph graph : Program.getGraphs()) {
			if (!used.contains(graph)) {
				freed.add(graph);
				graph.free();
			}
		}
		return freed;
	}

	private boolean localOptimizations(Iterable<Graph> graphs) {
		boolean anyChanged = false;
		for (Graph graph : graphs) {
			boolean changed;
			do {
				changed = false;
				for (GraphOptimizationStep<Graph, Boolean> step : this.graphOptimizationSteps) {
					changed |= step.optimize(graph);
				}
				anyChanged |= changed;
			} while (changed);
		}
		return anyChanged;
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
