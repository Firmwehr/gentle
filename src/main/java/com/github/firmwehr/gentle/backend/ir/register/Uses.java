package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class Uses {

	private final MutableGraph<IkeaNode> usesGraph;
	private final ControlFlowGraph controlFlow;

	public Uses(ControlFlowGraph controlFlow) {
		this.controlFlow = controlFlow;
		this.usesGraph = GraphBuilder.directed().allowsSelfLoops(true).build();

		recompute();
	}

	public void recompute() {
		usesGraph.edges().clear();
		usesGraph.nodes().clear();

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
