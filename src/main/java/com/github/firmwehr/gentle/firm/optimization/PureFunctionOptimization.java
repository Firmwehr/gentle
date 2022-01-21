package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Mut;
import firm.BackEdges;
import firm.Entity;
import firm.Graph;
import firm.Mode;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;
import firm.nodes.Store;

import java.util.HashSet;
import java.util.Set;

public class PureFunctionOptimization {
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
		// Possible impurities are:
		// 1. Endless loops
		// 2. Being in a call dependency loop (endless loop via recursion possible)
		// 3. Calling other impure functions
		// 4. Writing to memory
		//
		// 1, 3 and 4 are checked here while 2 is checked implicitly because all functions start out as impure and are
		// only set to pure if all their callees are already pure. This works because the functions are visited in
		// postorder.
		return !hasLoops(graph) && !hasSuspiciousNodes(graph);
	}

	private boolean hasLoops(Graph graph) {
		Mut<Boolean> hasLoops = new Mut<>(false);

		Set<Block> visited = new HashSet<>();
		graph.walkBlocksPostorder(block -> {
			for (Node pred : block.getPreds()) {
				// If a predecessor is not visited yet, we're in a cycle. This works because we visit the blocks in
				// postorder.
				if (pred.getBlock() instanceof Block predBlock) {
					if (!visited.contains(predBlock)) {
						hasLoops.set(true);
					}
				} else {
					throw new InternalCompilerException("block of node is actually not a block");
				}
			}
			visited.add(block);
		});

		return hasLoops.get();
	}

	private boolean hasSuspiciousNodes(Graph graph) {
		Mut<Boolean> result = new Mut<>(false);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Store node) {
				result.set(true);
			}

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
				if (!pureFunctions.contains(entity)) {
					return;
				}
				for (BackEdges.Edge out : BackEdges.getOuts(node)) {
					//noinspection StatementWithEmptyBody
					if (out.node instanceof Proj proj && proj.getMode().equals(Mode.getM())) {
						// All children of the call node are just memory projections
						// We can safely remove the call since we know the function is pure
					} else {
						return;
					}
				}
				for (BackEdges.Edge memProj : BackEdges.getOuts(node)) {
					for (BackEdges.Edge memUser : BackEdges.getOuts(memProj.node)) {
						memUser.node.setPred(memUser.pos, node.getMem());
					}
				}
				changed.set(true);
			}
		});
		BackEdges.disable(graph);
		return changed.get();
	}
}
