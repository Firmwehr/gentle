package com.github.firmwehr.gentle.firm.optimization.loops;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import firm.Graph;
import firm.nodes.Block;
import firm.nodes.Node;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class BlockBackedges {
	private final MutableGraph<Block> blocks;
	private final Block start;

	public BlockBackedges(Graph graph) {
		this.blocks = GraphBuilder.directed().allowsSelfLoops(true).build();
		this.start = graph.getStartBlock();

		graph.walkBlocks(block -> {
			for (Node pred : block.getPreds()) {
				if (pred.getBlock() instanceof Block predBlock) {
					blocks.putEdge(predBlock, block);
				}
			}
		});
	}

	public Set<Block> getOut(Block block) {
		return blocks.successors(block);
	}

	public Iterable<Block> breadthFirstFromStart() {
		return Traverser.forGraph(blocks).breadthFirst(start);
	}
}
