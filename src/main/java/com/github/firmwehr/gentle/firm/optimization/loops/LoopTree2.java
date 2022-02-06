package com.github.firmwehr.gentle.firm.optimization.loops;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.Sets;
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

	public LoopTree2(Graph graph) {
		this.graph = graph;
		this.tree = GraphBuilder.directed().allowsSelfLoops(true).build();

		this.blockBackedges = new BlockBackedges(graph);

		recompute();
	}

	private void recompute() {
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
				blocks.addAll(findBlocksBetween(tail, head));
			}

			headToInnerNodes.put(head, blocks);
		}

		for (var entry : headToInnerNodes.entrySet()) {
			for (Block inner : entry.getValue()) {
				tree.putEdge(entry.getKey(), inner);
			}
		}
	}

	private Set<Block> findBlocksBetween(Block tail, Block head) {
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
				// TODO: Do sth nicer.
				throw new InternalCompilerException("Found irreducible control flow");
			}

			worklist.addAll(preds);
		}

		blocks.add(head);

		return blocks;
	}

	public Optional<Block> getInnermostLoopHeader(Block block) {
		Set<Block> headers = getLoopHeaders(block);

		if (headers.isEmpty()) {
			return Optional.empty();
		}

		for (Block potentialHeader : headers) {
			Sets.SetView<Block> parents = Sets.intersection(tree.predecessors(potentialHeader), headers);
			// Our loop header has the same amount of parents as the block we are looking for. This should work, as
			// the header points to itself as well and therefore should have the same parents as hour block.
			if (parents.size() == headers.size()) {
				return Optional.of(potentialHeader);
			}
		}

		throw new InternalCompilerException("Did not find an innermost loop header for " + block);
	}

	public Set<Block> getLoopHeaders(Block block) {
		return Set.copyOf(tree.predecessors(block));
	}

	public Set<Node> getLoopBody(Block block) {
		return Set.copyOf(tree.successors(block));
	}

	public int getLoopDepth(Block block) {
		if (!tree.nodes().contains(block)) {
			return 0;
		}
		return tree.predecessors(block).size();
	}
}
