package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
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

	private final Graph<LegoPlate> graph;

	private ControlFlowGraph(Graph<LegoPlate> graph) {
		this.graph = graph;
	}

	/**
	 * @param block the block to get inputs for
	 *
	 * @return all blocks that might branch off into the passed block
	 */
	public Set<LegoPlate> inputBlocks(LegoPlate block) {
		return Collections.unmodifiableSet(graph.predecessors(block));
	}

	/**
	 * @param block the block to get outputs for
	 *
	 * @return all blocks {@code block} branches off to
	 */
	public Set<LegoPlate> outputBlocks(LegoPlate block) {
		return Collections.unmodifiableSet(graph.successors(block));
	}

	public Set<LegoPlate> getAllBlocks() {
		return Set.copyOf(graph.nodes());
	}

	public LegoPlate getStart() {
		return graph.nodes()
			.stream()
			.filter(it -> graph.inDegree(it) == 0)
			.findFirst()
			.orElseThrow(() -> new InternalCompilerException("Control flow graph has no start node: " + graph));
	}

	public List<LegoPlate> getEnds() {
		List<LegoPlate> ends = graph.nodes().stream().filter(it -> graph.outDegree(it) == 0).toList();

		if (ends.isEmpty()) {
			throw new InternalCompilerException("Control flow graph has no end node: " + graph);
		}

		return ends;
	}

	public List<LegoPlate> reversePostOrder() {
		List<LegoPlate> blocks = new ArrayList<>();
		for (LegoPlate block : Traverser.forGraph(graph).depthFirstPostOrder(getStart())) {
			blocks.add(block);
		}

		Collections.reverse(blocks);

		return blocks;
	}

	public static ControlFlowGraph forBlocks(List<LegoPlate> blocks) {
		MutableGraph<LegoPlate> graph = GraphBuilder.directed().allowsSelfLoops(true).build();

		for (LegoPlate block : blocks) {
			for (LegoParentBløck parent : block.parents()) {
				// reversed so an edge A -> B indicates control flow from A to B, not the other way round
				graph.putEdge(parent.parent(), block);
			}
		}

		blocks.forEach(graph::addNode);

		return new ControlFlowGraph(graph);
	}

}
