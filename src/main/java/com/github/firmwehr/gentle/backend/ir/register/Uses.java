package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class Uses {

	private final MutableGraph<IkeaNode> usesGraph;

	public Uses(ControlFlowGraph controlFlow) {
		this.usesGraph = GraphBuilder.directed().allowsSelfLoops(true).build();

		for (IkeaBløck block : controlFlow.getAllBlocks()) {
			for (IkeaNode node : block.nodes()) {
				for (IkeaNode parent : node.parents()) {
					usesGraph.putEdge(parent, node);
				}
			}
		}
	}

	public Set<IkeaNode> uses(IkeaNode node) {
		return usesGraph.successors(node);
	}
}
