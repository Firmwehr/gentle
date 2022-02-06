package com.github.firmwehr.gentle.firm.optimization.loops;

import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import firm.Graph;
import firm.nodes.Block;
import firm.nodes.Node;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class LoopTree2 {

	private static final Logger LOGGER = new Logger(LoopTree2.class);

	private final MutableGraph<Block> tree;
	private final Graph graph;
	private final BlockBackedges blockBackedges;

	private LoopTree2(Graph graph) {
		this.graph = graph;
		this.tree = GraphBuilder.directed().allowsSelfLoops(true).build();

		this.blockBackedges = new BlockBackedges(graph);
	}

	private boolean recompute() {
		Set.copyOf(tree.nodes()).forEach(tree::removeNode);

		Map<Block, Set<Block>> headToTails = new HashMap<>();
		graph.incBlockVisited();

		for (Block block : blockBackedges.breadthFirstFromStart()) {
			LOGGER.debug("Visiting %s", block);
			block.markBlockVisited();

			for (Block pred : blockBackedges.getOut(block)) {
				if (pred.blockVisited() && GentleBindings.block_dominates(pred.ptr, block.ptr)) {
					// Found backedge. So apparently are in the loop head and "pred" is some tail
					headToTails.computeIfAbsent(pred, ignored -> new HashSet<>()).add(block);
					LOGGER.debug("Added head->tail: %s -> %s", pred, block);
				}
			}
		}

		Map<Block, Set<Block>> headToInnerNodes = new HashMap<>();
		for (var entry : headToTails.entrySet()) {
			Block head = entry.getKey();
			Set<Block> blocks = new HashSet<>();

			for (Block tail : entry.getValue()) {
				Optional<Set<Block>> blocksBetween = findBlocksBetween(tail, head);
				if (blocksBetween.isEmpty()) {
					return false;
				}
				blocks.addAll(blocksBetween.get());
			}

			headToInnerNodes.put(head, blocks);
		}

		for (var entry : headToInnerNodes.entrySet()) {
			for (Block inner : entry.getValue()) {
				tree.putEdge(entry.getKey(), inner);
			}
		}

		return true;
	}

	private Optional<Set<Block>> findBlocksBetween(Block tail, Block head) {
		Set<Block> blocks = new HashSet<>();
		Queue<Block> worklist = new ArrayDeque<>();
		worklist.add(tail);
		tail.getGraph().incBlockVisited();

		head.markBlockVisited();

		while (!worklist.isEmpty()) {
			Block block = worklist.poll();
			if (block.blockVisited()) {
				continue;
			}
			block.markBlockVisited();
			blocks.add(block);

			Set<Block> preds = Util.predsStream(block)
				.map(Node::getBlock)
				.filter(Block.class::isInstance)
				.map(Block.class::cast)
				.collect(Collectors.toSet());

			if (preds.isEmpty()) {
				return Optional.empty();
			}

			worklist.addAll(preds);
		}

		blocks.add(head);

		return Optional.of(blocks);
	}

	public int getLoopDepth(Block block) {
		if (!tree.nodes().contains(block)) {
			return 0;
		}
		return tree.predecessors(block).size();
	}

	public static Optional<LoopTree2> forGraph(Graph graph) {
		LoopTree2 tree = new LoopTree2(graph);
		if (!tree.recompute()) {
			return Optional.empty();
		}

		return Optional.of(tree);
	}
}
