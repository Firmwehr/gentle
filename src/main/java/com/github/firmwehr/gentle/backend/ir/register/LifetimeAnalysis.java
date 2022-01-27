package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.firm.model.LoopTree;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.Lists;
import firm.nodes.Block;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class LifetimeAnalysis {

	private static final Logger LOGGER = new Logger(LifetimeAnalysis.class);

	private final Map<IkeaBløck, BlockLiveliness> liveness;
	private final ControlFlowGraph controlFlowGraph;

	public LifetimeAnalysis(ControlFlowGraph controlFlowGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.liveness = new HashMap<>();

		recompute();
	}

	public void recompute() {
		LOGGER.info("Building lifetimes");
		Set<IkeaBløck> seen = new HashSet<>();

		Queue<IkeaBløck> worklist = new ArrayDeque<>();
		worklist.add(controlFlowGraph.getEnd());

		// LIVE_out[final] = ∅, rest also initialized to this for now
		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			liveness.put(block, BlockLiveliness.forBlock(block));
		}

		while (!worklist.isEmpty()) {
			IkeaBløck block = worklist.poll();
			BlockLiveliness live = liveness.get(block);
			Set<BlockLiveliness> successors =
				controlFlowGraph.outputBlocks(block).stream().map(liveness::get).collect(Collectors.toSet());

			LOGGER.debug("Updating %s with successors %s", block, successors);

			if (live.update(successors) || seen.add(block)) {
				LOGGER.debug("Updating in/out again");
				worklist.addAll(controlFlowGraph.inputBlocks(block));
				worklist.addAll(controlFlowGraph.outputBlocks(block));
			}
		}
	}

	public Set<IkeaNode> getLiveOut(IkeaBløck block) {
		return Set.copyOf(liveness.get(block).liveOut());
	}

	public Set<IkeaNode> getLiveIn(IkeaBløck block, IkeaBløck parent) {
		return Set.copyOf(liveness.get(block).liveIn().get(parent));
	}

	public Set<IkeaNode> getAllLiveIn(IkeaBløck block) {
		return liveness.get(block).liveIn().values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
	}

	public int getLoopPressure(LoopTree loopTree, LoopTree.LoopElement loopElement) {
		Map<Block, IkeaBløck> blockMap = new HashMap<>();
		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			blockMap.put(block.origin(), block);
		}

		return getLoopPressure(loopTree, loopElement, blockMap);
	}

	private int getLoopPressure(LoopTree loopTree, LoopTree.LoopElement loopElement, Map<Block, IkeaBløck> blockMap) {
		int pressure = 0;
		for (LoopTree.LoopElement child : loopTree.getChildren(loopElement)) {
			int childPressure = switch (child) {
				case LoopTree.LoopElementLoop ignored -> getLoopPressure(loopTree, child);
				case LoopTree.LoopElementNode node -> getBlockPressure(blockMap.get((Block) node.firmNode()));
			};

			pressure = Math.max(childPressure, pressure);
		}

		return pressure;
	}

	private int getBlockPressure(IkeaBløck block) {
		Set<IkeaNode> live = getLiveOut(block);
		int maxLive = live.size();

		for (IkeaNode node : Lists.reverse(block.nodes())) {
			live.remove(node);
			live.addAll(node.parents());

			// TODO: Is this clobber handling enough?
			maxLive = Math.max(maxLive, live.size() + node.clobbered().size());
		}

		return maxLive;
	}

	public Set<IkeaNode> getLiveBefore(IkeaNode before) {
		Set<IkeaNode> live = getLiveOut(before.getBlock());

		for (IkeaNode node : Lists.reverse(before.getBlock().nodes())) {
			live.remove(node);
			live.addAll(node.parents());
		}

		return live;
	}

	private record BlockLiveliness(
		Set<IkeaNode> kill,
		Map<IkeaBløck, Set<IkeaNode>> liveIn,
		Set<IkeaNode> liveOut,
		IkeaBløck block
	) {

		private boolean update(Set<BlockLiveliness> successors) {
			boolean changed = false;

			// LIVE_in[s] = GEN[s] + (LIVE_out[S] _ KILL[s])
			for (IkeaNode node : liveOut) {
				if (!kill.contains(node)) {
					for (IkeaParentBløck parent : block.parents()) {
						changed |= liveIn.get(parent.parent()).add(node);
					}
				}
			}

			// LIVE_out[s] = union_succ p: LIVE_in[p]
			for (BlockLiveliness successor : successors) {
				changed |= liveOut.addAll(successor.liveIn().get(block));
			}

			return changed;
		}

		private static BlockLiveliness forBlock(IkeaBløck block) {
			return new BlockLiveliness(kill(block), gen(block), new HashSet<>(), block);
		}

		private static Set<IkeaNode> kill(IkeaBløck block) {
			return block.nodes()
				.stream()
				.filter(it -> !(it instanceof IkeaConst))
				.filter(it -> !(it.box() instanceof IkeaUnassignedBøx))
				.collect(Collectors.toSet());
		}

		private static Map<IkeaBløck, Set<IkeaNode>> gen(IkeaBløck block) {
			Map<IkeaBløck, Set<IkeaNode>> liveIns = new HashMap<>();
			for (IkeaParentBløck parent : block.parents()) {
				liveIns.put(parent.parent(), new HashSet<>());
			}

			for (IkeaNode node : block.nodes()) {
				if (node instanceof IkeaPhi phi) {
					for (Map.Entry<IkeaBløck, IkeaNode> entry : phi.getParents().entrySet()) {
						if (entry.getValue() instanceof IkeaConst ||
							entry.getValue().box() instanceof IkeaUnassignedBøx) {
							continue;
						}
						liveIns.get(entry.getKey()).add(entry.getValue());
					}
					continue;
				}

				node.parents()
					.stream()
					.filter(it -> !(it instanceof IkeaConst))
					.filter(it -> !(it.box() instanceof IkeaUnassignedBøx))
					.filter(it -> !block.nodes().contains(it))
					.forEach(parent -> {
						for (IkeaParentBløck parentBlock : block.parents()) {
							liveIns.get(parentBlock.parent()).add(parent);
						}
					});
			}
			return liveIns;
		}
	}
}
