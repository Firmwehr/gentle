package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
import com.github.firmwehr.gentle.firm.model.LoopTree;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Uses {

	private static final Logger LOGGER = new Logger(Uses.class);

	private final ControlFlowGraph controlFlow;
	private final LegoGraph legoGraph;

	public Uses(ControlFlowGraph controlFlow, LegoGraph legoGraph) {
		this.controlFlow = controlFlow;
		this.legoGraph = legoGraph;
	}

	public boolean isLastUse(LifetimeAnalysis liveliness, LegoNode def, LegoNode use) {
		if (liveliness.getLiveOut(use.block()).contains(def)) {
			return false;
		}
		for (LegoNode potentialUse : Lists.reverse(use.block().nodes())) {
			if (potentialUse.equals(use)) {
				return true;
			}
			if (potentialUse.inputs().contains(def)) {
				return false;
			}
		}

		throw new InternalCompilerException("Did not find node in its block");
	}

	public Set<LegoNode> uses(LegoNode node) {
		return legoGraph.getOutputs(node);
	}

	public Optional<NextUse> nextUse(
		LifetimeAnalysis liveliness, LoopTree loopTree, LegoNode def, LegoNode from, boolean excludeFrom
	) {
		return nextUse(liveliness, loopTree, def, from, excludeFrom, new HashSet<>());
	}

	private Optional<NextUse> nextUse(
		LifetimeAnalysis liveliness,
		LoopTree loopTree,
		LegoNode def,
		LegoNode from,
		boolean excludeFrom,
		Set<LegoPlate> visitedBlocks
	) {
		// We found an (in-)direct loop. No need to look in that block again, if there is a use in there we found it
		// already via a shorter path.
		if (!visitedBlocks.add(from.block())) {
			return Optional.empty();
		}

		int fromLoopDepth = loopTree.getBlockElement(from.block().origin()).depth();
		int fromIndex = from.block().nodes().indexOf(from);
		NextUse foundUse = null;

		// Try to find use in our block :)
		for (LegoNode use : uses(def)) {
			if (!use.block().equals(from.block())) {
				continue;
			}
			if (use instanceof LegoPhi) {
				continue;
			}

			int useIndex = use.block().nodes().indexOf(use);

			// Too early!
			if (useIndex < fromIndex) {
				continue;
			}
			// We found from but we don't want to return it, carry on...
			if (excludeFrom && use.equals(from)) {
				continue;
			}

			// Found the first or closer use :)
			if (foundUse == null || useIndex < foundUse.distance()) {
				foundUse = new NextUse(def, use, useIndex - fromIndex, fromLoopDepth);
			}
		}

		// Early search in same block was successful, we are all set
		if (foundUse != null) {
			return Optional.of(foundUse);
		}

		// No use after "from" found in "from"'s block *or* it was a phi. We need to go deeper?
		Optional<LegoNode> phiUsage = from.block()
			.nodes()
			.stream()
			.filter(it -> it instanceof LegoPhi)
			.flatMap(it -> it.inputs().stream())
			.filter(it -> it.equals(def))
			.findFirst();

		int distToEndOfBlock = from.block().nodes().size() - fromIndex;

		// TODO: Search for usages of the phi? That depends on whether the phi actually needs the node (i.e. it is
		//  *live* there). Maybe we also spill the whole phi and keep the arguments in spill slots, in which case this
		//  is NOT a use. That decision might not have happened here though.
		if (phiUsage.isPresent()) {
			return Optional.of(new NextUse(def, phiUsage.get(), distToEndOfBlock, fromLoopDepth));
		}

		// Not used in this block :( This is gonna get ugly, but we need to carry on to other blocks. We can not use
		// the uses info as we need to walk the control flow to find how far away we are.

		for (LegoPlate outputBlock : controlFlow.outputBlocks(from.block())) {
			if (!liveliness.getLiveIn(outputBlock, from.block()).contains(def)) {
				LOGGER.debug("Skipping next-use check for %s at %s -> %s boundary (not live-in)", def, from.block(),
					outputBlock);
				continue;
			}

			Optional<NextUse> childUse =
				nextUse(liveliness, loopTree, def, outputBlock.nodes().get(0), false, visitedBlocks);
			// The variable was live in, there *must* be a use somewhere! Maybe we found a loop and aborted traversal
			// into an already visited block?
			if (childUse.isEmpty()) {
				LOGGER.debug("No use for %s found at %s -> %s boundary. Loop edge found?", def, from.block(),
					outputBlock);
				continue;
			}

			NextUse adjustedChildUse = childUse.get();

			// We started deeper into the loop tree, so we found a use at an *out* edge
			if (fromLoopDepth > adjustedChildUse.outermostLoopDepth()) {
				int delta = fromLoopDepth - adjustedChildUse.outermostLoopDepth();
				LOGGER.debug("Use for %s encountered out edge (%s -> %s, delta %s) after %s -> %s boundary", def,
					fromLoopDepth, adjustedChildUse.outermostLoopDepth(), delta, from.block(), outputBlock);

				// We want to punish this use, the loop will be taken a lot more often than the exit. You only exit
				// once but all good loops iterate more than that!
				// Because I am stupid we just add an arbitrary constant. If this turns out to as stupid as it feels:
				// Hello future me, no need to thank me!

				// Every loop has 100 executions/nodes per level, so this is totally a good metric!
				int outEdgePunishment = delta * 100;
				adjustedChildUse = adjustedChildUse.withDistance(adjustedChildUse.distance() + outEdgePunishment);
			}

			if (foundUse == null || adjustedChildUse.distance() < foundUse.distance()) {
				foundUse = adjustedChildUse;
			}
		}

		// Hm. This sounds bad? Might happen if from is the only use and excludeFrom is set, but does that ever happen?
		// TODO: Does this happen?
		if (foundUse == null) {
			// I think this is fine... Happens if a node in our workset is dead :(
			return Optional.empty();
		}

		// Cap loop depth at the largest one. We don't really care if the next use is more deeply nested:
		// If the use is in the loop iteration, we need to keep it alive through *each* one, no matter if it is the
		// last
		// We do not want to give them some benefit, as we also always have a use at our "from" depth.
		if (foundUse.outermostLoopDepth() > fromLoopDepth) {
			foundUse = foundUse.withOutermostLoopDepth(fromLoopDepth);
		}

		// It was in a different block, so we need to go to the end too
		foundUse = foundUse.withDistance(foundUse.distance() + distToEndOfBlock);

		return Optional.of(foundUse);
	}

	public record NextUse(
		LegoNode def,
		LegoNode usage,
		int distance,
		int outermostLoopDepth
	) {
		public NextUse withOutermostLoopDepth(int outermostLoopDepth) {
			return new NextUse(def, usage, distance, outermostLoopDepth);
		}

		public NextUse withDistance(int distance) {
			return new NextUse(def, usage, distance, outermostLoopDepth);
		}

	}
}
