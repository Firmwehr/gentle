package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.firm.optimization.loops.LoopTree2;
import com.github.firmwehr.gentle.output.Logger;
import com.sun.jna.Pointer;
import firm.Graph;
import firm.bindings.binding_irdom;
import firm.bindings.binding_irnode;
import firm.bindings.binding_irop;
import firm.nodes.Block;
import firm.nodes.Cmp;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class LoopInvariantOptimization {

	private static final Logger LOGGER = new Logger(LoopInvariantOptimization.class);

	private final Graph graph;
	private final LoopTree2 loopTree;
	private boolean hasChanged;

	private LoopInvariantOptimization(Graph graph) {
		this.graph = graph;

		this.loopTree = new LoopTree2(graph);
	}

	public static GraphOptimizationStep<Graph, Boolean> loopInvariantOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("LoopInvariantOptimization")
			.withOptimizationFunction(graph -> {
				// prior optimizations might have messed up dom information, we need to recalculate it
				binding_irdom.compute_doms(graph.ptr);

				int runs = 0;
				while (true) {
					LoopInvariantOptimization loopInvariantOptimization = new LoopInvariantOptimization(graph);
					loopInvariantOptimization.applyLoopInvariantOptimization();

					if (!loopInvariantOptimization.hasChanged) {
						break;
					} else if (LOGGER.isDebugEnabled()) {
						dumpGraph(graph, "loopinvariant-iteration");
					}
					runs++;
				}
				boolean changed = runs > 0;
				if (changed) {
					dumpGraph(graph, "loopinvariant");
				}
				return changed;
			})
			.build();
	}

	public void applyLoopInvariantOptimization() {
		LOGGER.info("Searching for opportunities");
		Map<Node, Block> moveMap = new HashMap<>();

		graph.walk(new NodeVisitor.Default() {
			@Override
			public void defaultVisit(Node n) {
				Optional<Block> targetHeader = getTargetHeader(n);
				if (targetHeader.isEmpty()) {
					return;
				}

				Block insertBlock = getInsertBlock(n, targetHeader.get());
				LOGGER.debug("Selected insert block %s (%s) for %s (%s)", insertBlock,
					loopTree.getLoopDepth(insertBlock), n, loopTree.getLoopDepth((Block) n.getBlock()));
				moveMap.put(n, insertBlock);
			}
		});

		for (var entry : moveMap.entrySet()) {
			LOGGER.debug("Moving %s -> %s", entry.getKey(), entry.getValue());
			entry.getKey().setBlock(entry.getValue());
		}
		hasChanged = !moveMap.isEmpty();
	}

	private Block getInsertBlock(Node node, Block targetHeader) {
		int targetDepth = loopTree.getLoopDepth(targetHeader);

		Block next = (Block) node.getBlock();
		while (loopTree.getLoopDepth(next) > targetDepth) {
			next = (Block) Node.createWrapper(GentleBindings.get_Block_idom(next.ptr));
		}

		return next;
	}

	private Optional<Block> getTargetHeader(Node node) {
		if (isIgnore(node)) {
			return Optional.empty();
		}

		int depth = loopTree.getLoopDepth((Block) node.getBlock());
		if (depth == 0) {
			return Optional.empty();
		}
		int maxInvariantDepth = Integer.MIN_VALUE;
		Block maxInvariantBlock = null;

		for (Node pred : node.getPreds()) {
			Block predBlock = (Block) pred.getBlock();
			int predDepth = loopTree.getLoopDepth(predBlock);

			if (predDepth >= depth) {
				return Optional.empty();
			}
			if (predDepth > maxInvariantDepth) {
				LOGGER.debug("Changed max depth from %d to %d for %s", maxInvariantDepth, predDepth, node);
				maxInvariantBlock = predBlock;
				maxInvariantDepth = predDepth;
			}
		}

		return Optional.ofNullable(maxInvariantBlock);
	}

	private boolean isIgnore(Node node) {
		return hasControlflow(node) || hasMemory(node) || node.getPredCount() == 0 || isMarriedToParent(node);
	}

	private boolean isMarriedToParent(Node node) {
		return node instanceof Cmp || node instanceof Phi;
	}

	private boolean hasControlflow(Node node) {
		Pointer op = binding_irnode.get_irn_op(node.ptr);
		int flags = binding_irop.get_op_flags(op);
		return (flags & binding_irop.irop_flags.irop_flag_cfopcode.val) != 0;
	}

	private boolean hasMemory(Node node) {
		Pointer op = binding_irnode.get_irn_op(node.ptr);
		int flags = binding_irop.get_op_flags(op);
		return (flags & binding_irop.irop_flags.irop_flag_const_memory.val) != 0;
	}
}
