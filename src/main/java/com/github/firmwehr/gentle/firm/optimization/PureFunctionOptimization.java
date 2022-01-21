package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Mut;
import firm.BackEdges;
import firm.Entity;
import firm.Graph;
import firm.Mode;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Div;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;
import firm.nodes.Store;

import java.util.HashSet;
import java.util.Set;

public class PureFunctionOptimization {
	private static final Logger LOGGER = new Logger(PureFunctionOptimization.class);

	private final CallGraph callGraph;
	private final Set<Entity> pureFunctions;

	public PureFunctionOptimization(CallGraph callGraph) {
		this.callGraph = callGraph;

		pureFunctions = new HashSet<>();
	}

	public static GraphOptimizationStep<CallGraph, Set<Graph>> pureFunctionOptimization() {
		return GraphOptimizationStep.<CallGraph, Set<Graph>>builder()
			.withDescription("PureFunctionOptimization")
			.withOptimizationFunction(callGraph -> new PureFunctionOptimization(callGraph).optimize())
			.build();
	}

	private Set<Graph> optimize() {
		callGraph.walkPostorder(graph -> {
			if (isPure(graph)) {
				pureFunctions.add(graph.getEntity());
			}
		});

		System.out.println("Pure functions:");
		for (Entity pureFunction : pureFunctions) {
			System.out.println("  " + pureFunction.getLdName());
		}

		Set<Graph> modified = new HashSet<>();
		callGraph.walkPostorder(graph -> {
			if (removeUnusedPureCalls(graph)) {
				modified.add(graph);
			}
		});

		for (Graph graph : modified) {
			GraphDumper.dumpGraph(graph, "remove-pure");
		}
		return modified;
	}

	private boolean isPure(Graph graph) {
		return !hasLoops(graph) && !modifiesMemory(graph) && !callsImpureFunctions(graph);
	}

	private boolean hasLoops(Graph graph) {
		Mut<Boolean> result = new Mut<>(false);
		Set<Block> visited = new HashSet<>();
		graph.walkBlocksPostorder(block -> {
			//System.out.println("Looking at " + block.toString());
			for (Node pred : block.getPreds()) {
				if (pred.getBlock() instanceof Block predBlock) {
					//System.out.println("  Pred " + pred.toString() + " in " + predBlock.toString());
					if (!visited.contains(predBlock)) {
						//System.out.println("    Not visited yet, cycle detected");
						result.set(true);
					}
				} else {
					throw new InternalCompilerException("block of node is actually not a block");
				}
			}
			visited.add(block);
		});
		return result.get();
	}

	private boolean modifiesMemory(Graph graph) {
		Mut<Boolean> result = new Mut<>(false);
		graph.walk(new NodeVisitor.Default() {
			// Calls are handled in #callsImpureFunctions

			@Override
			public void visit(Div node) {
				result.set(true);
			}

			@Override
			public void visit(Mod node) {
				result.set(true);
			}

			@Override
			public void visit(Store node) {
				result.set(true);
			}
		});
		return result.get();
	}

	private boolean callsImpureFunctions(Graph graph) {
		Mut<Boolean> result = new Mut<>(false);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				Entity entity = ((Address) node.getPtr()).getEntity();
				// The stdlib functions are not in pureFunctions, so they're automatically considered impure.
				if (!pureFunctions.contains(entity)) {
					result.set(true);
				}
			}
		});
		return result.get();
	}

	private boolean removeUnusedPureCalls(Graph graph) {
		Mut<Boolean> changed = new Mut<>(false);
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				Entity entity = ((Address) node.getPtr()).getEntity();
				System.out.println("Call: " + entity.getLdName());
				if (!pureFunctions.contains(entity)) {
					return;
				}
				for (BackEdges.Edge out : BackEdges.getOuts(node)) {
					System.out.println("  Out-Edge: " + out.node);
					if (out.node instanceof Proj proj && proj.getMode().equals(Mode.getM())) {
						// All children of the call node are just memory projections
						// We can safely remove the call since we know the function is pure
					} else {
						return;
					}
				}
				System.out.println("  Removing...");
				for (BackEdges.Edge out : BackEdges.getOuts(node)) {
					out.node.setPred(out.pos, node.getMem());
				}
				changed.set(true);
			}
		});
		BackEdges.disable(graph);
		return changed.get();
	}
}
