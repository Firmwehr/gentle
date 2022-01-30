package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
		return Collections.unmodifiableSet(graph.predecessors(block));
	}

	/**
	 * @param block the block to get outputs for
	 *
	 * @return all blocks {@code block} branches off to
	 */
	public Set<IkeaBløck> outputBlocks(IkeaBløck block) {
		return Collections.unmodifiableSet(graph.successors(block));
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

	public List<IkeaBløck> getEnds() {
		List<IkeaBløck> ends = graph.nodes().stream().filter(it -> graph.outDegree(it) == 0).toList();

		if (ends.isEmpty()) {
			throw new InternalCompilerException("Control flow graph has no end node: " + graph);
		}

		return ends;
	}

	public List<IkeaBløck> reversePostOrder() {
		List<IkeaBløck> blocks = new ArrayList<>();
		for (IkeaBløck block : Traverser.forGraph(graph).depthFirstPostOrder(getStart())) {
			blocks.add(block);
		}

		Collections.reverse(blocks);

		return blocks;
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
