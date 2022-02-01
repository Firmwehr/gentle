package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConst;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
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

	private final Map<LegoPlate, BlockLiveliness> liveness;
	private final ControlFlowGraph controlFlowGraph;

	public LifetimeAnalysis(ControlFlowGraph controlFlowGraph) {
		this.controlFlowGraph = controlFlowGraph;
		this.liveness = new HashMap<>();

		recompute();
	}

	public void recompute() {
		LOGGER.info("Building lifetimes");
		Set<LegoPlate> seen = new HashSet<>();

		Queue<LegoPlate> worklist = new ArrayDeque<>(controlFlowGraph.getEnds());

		// LIVE_out[final] = ∅, rest also initialized to this for now
		for (LegoPlate block : controlFlowGraph.getAllBlocks()) {
			liveness.put(block, BlockLiveliness.forBlock(block));
		}

		while (!worklist.isEmpty()) {
			LegoPlate block = worklist.poll();
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

	public Set<LegoNode> getLiveOut(LegoPlate block) {
		return Set.copyOf(liveness.get(block).liveOut());
	}

	public Set<LegoNode> getLiveIn(LegoPlate block, LegoPlate parent) {
		return Set.copyOf(liveness.get(block).liveIn().get(parent));
	}

	public Set<LegoNode> getAllLiveIn(LegoPlate block) {
		return liveness.get(block).liveIn().values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
	}

	public int getLoopPressure(LoopTree loopTree, LoopTree.LoopElement loopElement) {
		Map<Block, LegoPlate> blockMap = new HashMap<>();
		for (LegoPlate block : controlFlowGraph.getAllBlocks()) {
			blockMap.put(block.origin(), block);
		}

		return getLoopPressure(loopTree, loopElement, blockMap);
	}

	private int getLoopPressure(LoopTree loopTree, LoopTree.LoopElement loopElement, Map<Block, LegoPlate> blockMap) {
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

	private int getBlockPressure(LegoPlate block) {
		Set<LegoNode> live = getLiveOut(block);
		int maxLive = live.size();

		for (LegoNode node : Lists.reverse(block.nodes())) {
			node.results().forEach(live::remove);
			live.addAll(node.inputs());

			// TODO: Is this clobber handling enough?
			maxLive = Math.max(maxLive, live.size() + node.clobbered().size());
		}

		return maxLive;
	}

	public Set<LegoNode> getLiveBefore(LegoNode before) {
		Set<LegoNode> live = new HashSet<>(getLiveOut(before.block()));

		for (LegoNode node : Lists.reverse(before.block().nodes())) {
			node.results().forEach(live::remove);
			live.addAll(node.inputs());
			if (node.equals(before)) {
				break;
			}
		}

		return live;
	}

	private record BlockLiveliness(
		Set<LegoNode> kill,
		Map<LegoPlate, Set<LegoNode>> liveIn,
		Set<LegoNode> liveOut,
		LegoPlate block
	) {

		private boolean update(Set<BlockLiveliness> successors) {
			boolean changed = false;

			// LIVE_in[s] = GEN[s] + (LIVE_out[S] _ KILL[s])
			for (LegoNode node : liveOut) {
				if (!kill.contains(node)) {
					for (LegoParentBløck parent : block.parents()) {
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

		private static BlockLiveliness forBlock(LegoPlate block) {
			return new BlockLiveliness(kill(block), gen(block), new HashSet<>(), block);
		}

		private static Set<LegoNode> kill(LegoPlate block) {
			return block.nodes()
				.stream()
				.filter(it -> !(it instanceof LegoConst))
				.filter(it -> !it.registerIgnore())
				.collect(Collectors.toSet());
		}

		private static Map<LegoPlate, Set<LegoNode>> gen(LegoPlate block) {
			Map<LegoPlate, Set<LegoNode>> liveIns = new HashMap<>();
			for (LegoParentBløck parent : block.parents()) {
				liveIns.put(parent.parent(), new HashSet<>());
			}

			for (LegoNode node : block.nodes()) {
				if (node instanceof LegoPhi phi) {
					for (LegoParentBløck parentBlock : block.parents()) {
						LegoNode parentValue = phi.parent(parentBlock.parent());
						if (parentValue instanceof LegoConst || parentValue.registerIgnore()) {
							continue;
						}
						liveIns.get(parentBlock.parent()).add(parentValue);
					}
					continue;
				}

				node.inputs()
					.stream()
					.filter(it -> !it.registerIgnore())
					.filter(it -> !(it instanceof LegoConst))
					.filter(it -> !block.nodes().contains(it))
					.forEach(parent -> {
						for (LegoParentBløck parentBlock : block.parents()) {
							liveIns.get(parentBlock.parent()).add(parent);
						}
					});
			}
			return liveIns;
		}
	}
}
