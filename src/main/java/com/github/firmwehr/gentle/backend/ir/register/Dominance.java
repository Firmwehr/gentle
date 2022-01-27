package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class Dominance {

	private static final Logger LOGGER = new Logger(Dominance.class, Logger.LogLevel.DEBUG);

	private final MutableGraph<IkeaNode> dominatorTree;
	private final MutableGraph<IkeaBløck> blockDominators;
	private final MutableGraph<IkeaBløck> dominanceFrontier;
	private final ControlFlowGraph controlFlowGraph;

	private Dominance(ControlFlowGraph controlFlowGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.blockDominators = GraphBuilder.directed().allowsSelfLoops(true).build();
		this.dominatorTree = GraphBuilder.directed().allowsSelfLoops(true).build();
		this.dominanceFrontier = GraphBuilder.directed().allowsSelfLoops(true).build();

		recompute();
	}

	public static Dominance forCfg(ControlFlowGraph graph) {
		return new Dominance(graph);
	}

	public void recompute() {
		computeDominance();
		computeDominanceFrontier();
	}

	private void computeDominance() {
		LOGGER.info("Building dominance graph");
		Queue<IkeaBløck> worklist = new ArrayDeque<>();
		worklist.add(controlFlowGraph.getStart());

		while (!worklist.isEmpty()) {
			IkeaBløck block = worklist.poll();

			LOGGER.debug("Looking at %s", block);

			Set<IkeaBløck> blockDominators = controlFlowGraph.inputBlocks(block)
				.stream()
				.filter(it -> this.blockDominators.nodes().contains(it))
				.map(this.blockDominators::predecessors)
				.reduce(Set.of(), (a, b) -> a.isEmpty() ? b : Sets.intersection(a, b));
			LOGGER.debug("Block dominators %s", blockDominators);
			blockDominators = new HashSet<>(blockDominators);
			blockDominators.add(block);

			if (this.blockDominators.nodes().contains(block) &&
				blockDominators.equals(this.blockDominators.predecessors(block))) {
				LOGGER.debug("Was up to date");
				continue;
			}
			for (IkeaBløck dominator : blockDominators) {
				this.blockDominators.putEdge(dominator, block);
			}
			worklist.addAll(controlFlowGraph.outputBlocks(block));
			LOGGER.debug("Adding succesors %s", controlFlowGraph.outputBlocks(block));
		}

		for (IkeaBløck block : blockDominators.nodes()) {
			LOGGER.debugHeader("Dominators for %s", block);
			for (IkeaBløck dominator : blockDominators.predecessors(block)) {
				LOGGER.debug("%s", dominator);
			}
		}

		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			Set<IkeaBløck> dominators = this.blockDominators.predecessors(block);

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

	private void computeDominanceFrontier() {
		// https://www.ed.tus.ac.jp/j-mune/keio/m/ssa2.pdf

		LOGGER.info("Building dominance frontier");
		Map<IkeaBløck, Set<IkeaBløck>> frontiers = new HashMap<>();

		for (IkeaBløck block : Traverser.forGraph(blockDominators).depthFirstPostOrder(controlFlowGraph.getStart())) {
			frontiers.put(block, new HashSet<>());
			LOGGER.debug("Explored %s", block);

			// local set: Successors we do not strictly dominate
			for (IkeaBløck succ : controlFlowGraph.outputBlocks(block)) {
				// We do not strictly dominate them
				if (!blockDominators.hasEdgeConnecting(block, succ)) {
					LOGGER.debug("Added %s to local set for %s", succ, block);
					frontiers.get(block).add(succ);
				}
			}

			// up set
			for (IkeaBløck succ : blockDominators.successors(block)) {
				if (succ.equals(block)) {
					continue;
				}
				if (!frontiers.containsKey(succ)) {
					throw new InternalCompilerException("Did not find precomputed frontier for " + succ);
				}

				for (IkeaBløck y : frontiers.get(succ)) {
					if (!blockDominators.hasEdgeConnecting(block, y)) {
						LOGGER.debug("Added %s to up set of %s", y, block);
						frontiers.get(block).add(y);
					}
				}
			}
		}

		for (Map.Entry<IkeaBløck, Set<IkeaBløck>> entry : frontiers.entrySet()) {
			dominanceFrontier.addNode(entry.getKey());
			for (IkeaBløck frontierNode : entry.getValue()) {
				dominanceFrontier.putEdge(entry.getKey(), frontierNode);
			}
		}
	}

	public Set<IkeaBløck> getDirectlyDominatedBlocks(IkeaBløck block) {
		return blockDominators.successors(block)
			.stream()
			.filter(it -> getIdom(it).orElseThrow().equals(block))
			.collect(Collectors.toSet());
	}

	public Optional<IkeaBløck> getIdom(IkeaBløck block) {
		Set<IkeaBløck> dominators = new HashSet<>(blockDominators.predecessors(block));
		dominators.remove(block);
		// TODO: Do we want to explicitly model self-dominance?
		if (dominators.isEmpty()) {
			return Optional.empty();
		}
		for (IkeaBløck possibleIdom : dominators) {
			// Dominator dominated by all others
			if (dominators.stream().allMatch(it -> blockDominators.hasEdgeConnecting(it, possibleIdom))) {
				return Optional.of(possibleIdom);
			}
		}
		throw new InternalCompilerException("No idom found?");
	}

	public Set<IkeaBløck> getDominanceFrontier(IkeaBløck root) {
		return Collections.unmodifiableSet(dominanceFrontier.successors(root));
	}

	public boolean dominates(IkeaNode first, IkeaNode second) {
		return dominatorTree.hasEdgeConnecting(first, second);
	}
}
