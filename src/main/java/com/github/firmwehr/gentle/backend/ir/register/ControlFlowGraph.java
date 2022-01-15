package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * A control flow graph for blocks.
 */
@SuppressWarnings("UnstableApiUsage")
public class ControlFlowGraph {

	private final Graph<IkeaBløck> graph;

	private ControlFlowGraph(Graph<IkeaBløck> graph) {
		this.graph = graph;
	}

	/**
	 * @param block the block to get inputs for
	 *
	 * @return all blocks that might branch off into the passed block
	 */
	public Set<IkeaBløck> inputBlocks(IkeaBløck block) {
		return graph.predecessors(block);
	}

	/**
	 * @param block the block to get outputs for
	 *
	 * @return all blocks {@code block} branches off to
	 */
	public Set<IkeaBløck> outputBlocks(IkeaBløck block) {
		return graph.successors(block);
	}

	public Set<IkeaBløck> getAllBlocks() {
		return Set.copyOf(graph.nodes());
	}

	public IkeaBløck getStart() {
		return graph.nodes()
			.stream()
			.filter(it -> graph.inDegree(it) == 0)
			.findFirst()
			.orElseThrow(() -> new InternalCompilerException("Control flow graph has no start node: " + graph));
	}

	public IkeaBløck getEnd() {
		return graph.nodes()
			.stream()
			.filter(it -> graph.outDegree(it) == 0)
			.findFirst()
			.orElseThrow(() -> new InternalCompilerException("Control flow graph has no end node: " + graph));
	}

	/**
	 * Constructs a new control flow graph starting at the given end block. Only blocks reachable from it are
	 * considered.
	 *
	 * @param endBlock the end block
	 *
	 * @return the constructed control flow graph
	 */
	public static ControlFlowGraph forEndBlock(IkeaBløck endBlock) {
		MutableGraph<IkeaBløck> graph = GraphBuilder.directed().allowsSelfLoops(true).build();

		Set<IkeaBløck> visited = new HashSet<>();
		Queue<IkeaBløck> blocks = new ArrayDeque<>();
		blocks.add(endBlock);

		while (!blocks.isEmpty()) {
			IkeaBløck block = blocks.poll();
			if (!visited.add(block)) {
				continue;
			}

			for (IkeaParentBløck parent : block.parents()) {
				// reversed so an edge A -> B indicates control flow from A to B, not the other way round
				graph.putEdge(parent.parent(), block);
			}
		}

		return new ControlFlowGraph(graph);
	}

	public static ControlFlowGraph forBlocks(List<IkeaBløck> blocks) {
		MutableGraph<IkeaBløck> graph = GraphBuilder.directed().allowsSelfLoops(true).build();

		for (IkeaBløck block : blocks) {
			for (IkeaParentBløck parent : block.parents()) {
				// reversed so an edge A -> B indicates control flow from A to B, not the other way round
				graph.putEdge(parent.parent(), block);
			}
		}

		blocks.forEach(graph::addNode);

		return new ControlFlowGraph(graph);
	}

}
