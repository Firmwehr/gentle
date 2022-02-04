package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
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

import static java.util.function.Predicate.not;

@SuppressWarnings("UnstableApiUsage")
public class Dominance {

	private static final Logger LOGGER = new Logger(Dominance.class, Logger.LogLevel.DEBUG);

	private final MutableGraph<LegoNode> dominatorTree;
	private final MutableGraph<LegoPlate> blockDominators;
	private final MutableGraph<LegoPlate> dominanceFrontier;
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
		blockDominators.nodes().stream().toList().forEach(blockDominators::removeNode);
		dominatorTree.nodes().stream().toList().forEach(dominatorTree::removeNode);
		dominanceFrontier.nodes().stream().toList().forEach(dominanceFrontier::removeNode);

		computeDominance();
		computeDominanceFrontier();
	}

	private void computeDominance() {
		LOGGER.info("Building dominance graph");
		Queue<LegoPlate> worklist = new ArrayDeque<>();
		worklist.add(controlFlowGraph.getStart());

		while (!worklist.isEmpty()) {
			LegoPlate block = worklist.poll();

			LOGGER.debug("Looking at %s", block);

			Set<LegoPlate> blockDominators = controlFlowGraph.inputBlocks(block)
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
			for (LegoPlate dominator : blockDominators) {
				this.blockDominators.putEdge(dominator, block);
			}
			worklist.addAll(controlFlowGraph.outputBlocks(block));
			LOGGER.debug("Adding successors %s", controlFlowGraph.outputBlocks(block));
		}

		for (LegoPlate block : blockDominators.nodes()) {
			LOGGER.debugHeader("Dominators for %s", block);
			for (LegoPlate dominator : blockDominators.predecessors(block)) {
				LOGGER.debug("%s", dominator);
			}
		}

		for (LegoPlate block : controlFlowGraph.getAllBlocks()) {
			// Model strict dominance
			this.blockDominators.removeEdge(block, block);

			Set<LegoPlate> dominators = this.blockDominators.predecessors(block);

			// Handle dominance from other blocks
			Set<LegoNode> nodesInDominators = dominators.stream()
				.filter(it -> !it.equals(block))
				.flatMap(it -> it.nodes().stream())
				.collect(Collectors.toSet());
			for (LegoNode node : block.nodes()) {
				for (LegoNode nodeInDominator : nodesInDominators) {
					dominatorTree.putEdge(nodeInDominator, node);
				}
			}

			// Handle dominance within our block
			List<LegoNode> visitedNodes = new ArrayList<>();
			for (LegoNode node : block.nodes()) {
				visitedNodes.add(node);
				for (LegoNode visitedNode : visitedNodes) {
					if (visitedNode.equals(node)) {
						continue;
					}
					dominatorTree.putEdge(visitedNode, node);
				}
			}
		}
		for (LegoNode node : dominatorTree.nodes()) {
			LOGGER.debugHeader("%s dominates", node.underlyingFirmNodes());
			LOGGER.debug("-> %s", dominatorTree.successors(node).stream().map(LegoNode::underlyingFirmNodes).toList());
		}
	}

	private void computeDominanceFrontier() {
		// https://www.ed.tus.ac.jp/j-mune/keio/m/ssa2.pdf

		LOGGER.info("Building dominance frontier");
		Map<LegoPlate, Set<LegoPlate>> frontiers = new HashMap<>();

		for (LegoPlate block : Traverser.forGraph(blockDominators).depthFirstPostOrder(controlFlowGraph.getStart())) {
			frontiers.put(block, new HashSet<>());
			LOGGER.debug("Explored %s", block);

			// local set: Successors we do not strictly dominate
			for (LegoPlate succ : controlFlowGraph.outputBlocks(block)) {
				// We do not strictly dominate them
				if (!blockDominators.hasEdgeConnecting(block, succ)) {
					LOGGER.debug("Added %s to local set for %s", succ, block);
					frontiers.get(block).add(succ);
				}
			}

			// up set
			for (LegoPlate succ : blockDominators.successors(block)) {
				if (succ.equals(block)) {
					continue;
				}
				if (!frontiers.containsKey(succ)) {
					throw new InternalCompilerException("Did not find precomputed frontier for " + succ);
				}

				for (LegoPlate y : frontiers.get(succ)) {
					if (!blockDominators.hasEdgeConnecting(block, y)) {
						LOGGER.debug("Added %s to up set of %s", y, block);
						frontiers.get(block).add(y);
					}
				}
			}
		}

		for (Map.Entry<LegoPlate, Set<LegoPlate>> entry : frontiers.entrySet()) {
			dominanceFrontier.addNode(entry.getKey());
			for (LegoPlate frontierNode : entry.getValue()) {
				dominanceFrontier.putEdge(entry.getKey(), frontierNode);
			}
		}
	}

	public Set<LegoPlate> getDirectlyDominatedBlocks(LegoPlate block) {
		return blockDominators.successors(block)
			.stream()
			.filter(not(block::equals))
			.filter(it -> getIdom(it).orElseThrow().equals(block))
			.collect(Collectors.toSet());
	}

	public Optional<LegoPlate> getIdom(LegoPlate block) {
		Set<LegoPlate> dominators = new HashSet<>(blockDominators.predecessors(block));
		dominators.remove(block);
		// TODO: Do we want to explicitly model self-dominance?
		if (dominators.isEmpty()) {
			return Optional.empty();
		}
		for (LegoPlate possibleIdom : dominators) {
			// Dominator dominated by all others
			boolean dominatedByAll = dominators.stream()
				.filter(it -> !it.equals(possibleIdom))
				.allMatch(it -> blockDominators.hasEdgeConnecting(it, possibleIdom));
			if (dominatedByAll) {
				return Optional.of(possibleIdom);
			}
		}
		throw new InternalCompilerException("No idom found?");
	}

	public Set<LegoPlate> getDominanceFrontier(LegoPlate root) {
		return Collections.unmodifiableSet(dominanceFrontier.successors(root));
	}

	public boolean dominates(LegoNode first, LegoNode second) {
		return dominatorTree.hasEdgeConnecting(first, second);
	}
}
