package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class Dominance {

	private static final Logger LOGGER = new Logger(Dominance.class, Logger.LogLevel.DEBUG);

	private final MutableGraph<IkeaNode> dominatorTree;
	private final Map<IkeaBløck, Set<IkeaBløck>> blockDominators;
	private final ControlFlowGraph controlFlowGraph;

	public Dominance(ControlFlowGraph controlFlowGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.blockDominators = new HashMap<>();
		this.dominatorTree = GraphBuilder.directed().allowsSelfLoops(true).build();
	}

	public void computeDominance() {
		Queue<IkeaBløck> worklist = new ArrayDeque<>();
		worklist.add(controlFlowGraph.getStart());

		while (!worklist.isEmpty()) {
			IkeaBløck block = worklist.poll();

			LOGGER.debug("Looking at %s", block);

			Set<IkeaBløck> blockDominators = controlFlowGraph.inputBlocks(block)
				.stream()
				.map(this.blockDominators::get)
				.filter(Objects::nonNull)
				.reduce(Set.of(), (a, b) -> a.isEmpty() ? b : Sets.intersection(a, b));
			LOGGER.debug("Block dominators %s", blockDominators);
			blockDominators = new HashSet<>(blockDominators);
			blockDominators.add(block);

			if (blockDominators.equals(this.blockDominators.get(block))) {
				LOGGER.debug("Was up to date");
				continue;
			}
			this.blockDominators.put(block, blockDominators);
			worklist.addAll(controlFlowGraph.outputBlocks(block));
			LOGGER.debug("Adding succesors %s", controlFlowGraph.outputBlocks(block));
		}

		for (Map.Entry<IkeaBløck, Set<IkeaBløck>> entry : blockDominators.entrySet()) {
			LOGGER.debugHeader("Dominators for %s", entry.getKey());
			for (IkeaBløck block : entry.getValue()) {
				LOGGER.debug("%s", block);
			}
		}

		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			Set<IkeaBløck> dominators = this.blockDominators.get(block);

			// Handle dominance from other blocks
			Set<IkeaNode> nodesInDominators = dominators.stream()
				.filter(it -> !it.equals(block))
				.flatMap(it -> it.nodes().stream())
				.collect(Collectors.toSet());
			for (IkeaNode node : block.nodes()) {
				for (IkeaNode nodeInDominator : nodesInDominators) {
					dominatorTree.putEdge(nodeInDominator, node);
				}
			}

			// Handle dominance within our block
			List<IkeaNode> visitedNodes = new ArrayList<>();
			for (IkeaNode node : block.nodes()) {
				visitedNodes.add(node);
				for (IkeaNode visitedNode : visitedNodes) {
					dominatorTree.putEdge(visitedNode, node);
				}
			}
		}
		for (IkeaNode node : dominatorTree.nodes()) {
			LOGGER.debugHeader("%s dominates", node.getUnderlyingFirmNodes());
			LOGGER.debug("-> %s",
				dominatorTree.successors(node).stream().map(IkeaNode::getUnderlyingFirmNodes).toList());
		}
	}

	public boolean dominates(IkeaNode first, IkeaNode second) {
		return dominatorTree.hasEdgeConnecting(first, second);
	}
}
