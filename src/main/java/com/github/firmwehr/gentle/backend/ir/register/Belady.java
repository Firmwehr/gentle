package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaReload;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSpill;
import com.github.firmwehr.gentle.firm.model.LoopTree;
import com.github.firmwehr.gentle.output.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Belady {

	private static final Logger LOGGER = new Logger(Belady.class, Logger.LogLevel.DEBUG);

	private final Map<IkeaBløck, Set<WorksetNode>> startWorksets;
	private final Map<IkeaBløck, Set<WorksetNode>> endWorksets;
	private final Map<IkeaNode, SpillInfo> spillInfos;
	private final Dominance dominance;
	private final ControlFlowGraph controlFlow;
	private final LifetimeAnalysis liveliness;
	private final Uses uses;
	private final LoopTree loopTree;

	public Belady(
		Dominance dominance, ControlFlowGraph controlFlow, LifetimeAnalysis liveliness, Uses uses, LoopTree loopTree
	) {
		this.dominance = dominance;
		this.controlFlow = controlFlow;
		this.liveliness = liveliness;
		this.uses = uses;
		this.loopTree = loopTree;

		this.startWorksets = new HashMap<>();
		this.endWorksets = new HashMap<>();
		this.spillInfos = new HashMap<>();
	}

	public void spill(ControlFlowGraph graph) {
		for (IkeaBløck block : graph.reversePostOrder()) {
			processBlock(block);
		}

		for (IkeaBløck block : graph.getAllBlocks()) {
			fixBlockBorder(block);
		}

		// Also fixes SSA and invalidates liveliness, uses and dominance :awesome:
		realizeSpillsAndReloads();

		assignSpillSlots();
		// TODO: Spill slot coalescing. We might be able to put multiple different values in the same spill slot (e.g.
		//  for spilled phis) which would eliminate moves. I am not certain we can actually express this in our
		//  garbage backend though, as we'd need a way to determine whether a value is live that treats reloads as
		//  live too!
	}

	private void processBlock(IkeaBløck block) {
		if (startWorksets.containsKey(block)) {
			throw new InternalCompilerException("Visited a block twice?");
		}
		startWorksets.put(block, decideStartWorkset(block));

		LOGGER.debugHeader("Start workset for %s", block.id());
		LOGGER.debug("  %s", startWorksets.get(block));

		// This will become out end workset
		Set<WorksetNode> currentBlockWorkset = new HashSet<>(startWorksets.get(block));

		for (IkeaNode node : block.nodes()) {
			// Not an instruction, only relevant for deciding our start worksets and wiring them up correctly
			// Not keeping track of our phi inputs might cause additional register demand when translating phis, but
			// due to exchange instructions on x86 this should be fine :^)
			if (node instanceof IkeaPhi) {
				continue;
			}

			// We need all our inputs in registers here
			displace(node.parents(), currentBlockWorkset, node, true);
			displace(Set.of(node), currentBlockWorkset, node, false);
		}

		endWorksets.put(block, currentBlockWorkset);
	}

	private void displace(
		Collection<IkeaNode> newValues, Set<WorksetNode> currentWorkset, IkeaNode currentInstruction, boolean isUsage
	) {
		LOGGER.debug("Making room for %s after %s (usage: %s). Live: %s", newValues, currentInstruction, isUsage,
			currentWorkset);
		int additionalPressure = 1; // We do not use all registers (sp is not a good idea?)
		Set<WorksetNode> toInsert = new HashSet<>();

		int demand = newValues.size();
		for (IkeaNode value : newValues) {
			WorksetNode worksetValue = new WorksetNode(value);
			// Needs a reload!
			if (!currentWorkset.contains(worksetValue) && isUsage) {
				// add a reload here!
				LOGGER.debug("Adding reload for %s before %s", value, currentInstruction);
				addReload(value, currentInstruction);
				worksetValue.setSpilled(true);
			} else {
				if (!isUsage) {
					throw new InternalCompilerException("Already knew a value I am introducing?");
				}
				// Remove so it is not accidentally selected for spilling
				currentWorkset.remove(worksetValue);
				LOGGER.debug("%s was already live before %s", value, currentInstruction);
			}
			// TODO: Is this clobber handling good enough?
			additionalPressure += value.clobbered().size();
			if (!value.clobbered().isEmpty()) {
				LOGGER.debug("Increased %s pressure by %s clobbers", value, value.clobbered().size());
			}
			toInsert.add(worksetValue);
		}

		demand += additionalPressure;

		// e.g. 10 Registers in X86, demand ist 4 and size is 4
		// 8 - 10 => -2 => No spills needed
		int neededSpills = demand + currentWorkset.size() - X86Register.registerCount();

		if (neededSpills > 0) {
			LOGGER.debug("Need to make room for %s values", neededSpills);

			for (WorksetNode node : currentWorkset) {
				// If we add a value defined by the instruction, we do not want to include that node, though it should
				// not be a use anyways.
				Optional<Integer> distance =
					uses.nextUse(liveliness, loopTree, node.node(), currentInstruction, !isUsage)
						.map(Uses.NextUse::distance);
				if (distance.isPresent()) {
					node.setDistance(new Distance(distance.get()));
				} else {
					// No use found...?
					node.setDistance(new Infinity());
				}
			}

			List<WorksetNode> sortedNodes = currentWorkset.stream().sorted(Collections.reverseOrder()).toList();

			LOGGER.debug("Workset for spill: ");
			LOGGER.debug("  %s",
				sortedNodes.stream().map(it -> it.node() + ": " + it.distance()).collect(Collectors.joining(", ")));

			for (int i = 0; i < neededSpills; i++) {
				WorksetNode victim = sortedNodes.get(i);
				IkeaNode victimNode = victim.node();
				currentWorkset.remove(victim);
				LOGGER.debug("Spilling %s before %s", victim, currentInstruction);
				IkeaNode victimParent =
					victimNode.getBlock().nodes().get(victimNode.getBlock().nodes().indexOf(victimNode) - 1);

				if (victim.spilled() || victim.distance() instanceof Infinity) {
					LOGGER.debug("Skipping spill for %s due to distance/spilled status", victim);
					continue;
				}
				LOGGER.debug("Spilling %s after encountering %s", victim, currentInstruction);
				addSpill(victimNode, victimParent);
			}
		}

		currentWorkset.addAll(toInsert);
	}

	private void addReload(IkeaNode valueToReload, IkeaNode before) {
		SpillInfo spillInfo = spillInfos.computeIfAbsent(valueToReload, SpillInfo::forNode);
		spillInfo.reloadBefore().add(before);
	}

	private void addSpill(IkeaNode valueToSpill, IkeaNode after) {
		SpillInfo spillInfo = spillInfos.computeIfAbsent(valueToSpill, SpillInfo::forNode);

		for (Iterator<IkeaNode> iterator = spillInfo.toSpillAfter().iterator(); iterator.hasNext(); ) {
			IkeaNode existingAfter = iterator.next();
			// No need to spill if a spill already dominates us. We spill the same value and always walk past that
			// spill.
			if (dominance.dominates(existingAfter, after)) {
				LOGGER.debug("Spill for %s after %s was already dominated by %s", valueToSpill, after, existingAfter);
				return;
			}
			// No need to keep the old spill if we dominate it!
			if (dominance.dominates(after, existingAfter)) {
				LOGGER.debug("Removed spill for %s after %s as it was dominated by %s", valueToSpill, existingAfter,
					after);
				iterator.remove();
			}
		}

		spillInfo.toSpillAfter().add(after);
	}

	private Set<WorksetNode> decideStartWorkset(IkeaBløck block) {
		if (block.parents().isEmpty()) {
			return new HashSet<>();
		}
		if (block.parents().size() == 1) {
			return new HashSet<>(endWorksets.get(block.parents().get(0).parent()));
		}

		return magicStartWorkset(block);
	}

	private Set<WorksetNode> magicStartWorkset(IkeaBløck block) {
		// Nodes we definitely want to keep if possible
		Set<WorksetNode> starters = new HashSet<>();
		// Nodes we would like to keep but can compromise on. They are used later on or in less nested loops, etc.
		Set<WorksetNode> delayed = new HashSet<>();

		// Decide whether to spill or keep Phi
		for (IkeaNode phi : block.nodes().stream().filter(it -> it instanceof IkeaPhi).toList()) {
			PredecessorAvailability availabiity = computePredecessorAvailability(block, phi);

			WorksetNode worksetNode = identityCrisis(block, phi, availabiity);
			switch (worksetNode.distance()) {
				case LoopDelayed ignored -> delayed.add(worksetNode);
				case Distance ignored -> starters.add(worksetNode);
				case Infinity ignored -> addPhiSpill((IkeaPhi) phi, block);
				case UnknownDist ignored -> throw new InternalCompilerException("Unknown use distance!");
			}
		}

		List<IkeaNode> liveIn =
			block.parents().stream().flatMap(it -> liveliness.getLiveIn(block, it.parent()).stream()).toList();
		for (IkeaNode node : liveIn) {
			PredecessorAvailability availability = computePredecessorAvailability(block, node);

			WorksetNode worksetNode = identityCrisis(block, node, availability);
			switch (worksetNode.distance()) {
				case LoopDelayed ignored -> delayed.add(worksetNode);
				case Distance ignored -> starters.add(worksetNode);
				case Infinity ignored -> throw new InternalCompilerException("Why you dead?");
				case UnknownDist ignored -> throw new InternalCompilerException("Unknown use distance!");
			}
		}

		int loopPressure = liveliness.getLoopPressure(loopTree, loopTree.getBlockElement(block.origin()));
		int freeSlots = X86Register.registerCount() - starters.size();
		int freePressureSlots = X86Register.registerCount() - (loopPressure - delayed.size());
		freeSlots = Math.min(freeSlots, freePressureSlots);

		if (freeSlots > 0) {
			int takenSlots = 0;
			for (WorksetNode worksetNode : delayed.stream().sorted().toList()) {
				if (takenSlots >= freeSlots) {
					break;
				}
				if (!(worksetNode.node() instanceof IkeaPhi)) {
					PredecessorAvailability availability = computePredecessorAvailability(block, worksetNode.node());
					if (availability != PredecessorAvailability.LIVE_IN_ALL) {
						LOGGER.debug("Delayed node %s was not live in all preds, skipping it", worksetNode);
						// Do not unnecessarily reload stuff
						continue;
					}
				}

				LOGGER.debug("Taking delayed node %s", worksetNode);
				starters.add(worksetNode);
				takenSlots++;
			}
		}

		for (WorksetNode worksetNode : delayed) {
			if (worksetNode.node() instanceof IkeaPhi) {
				// Spill the whole phi if we did not take it
				addPhiSpill((IkeaPhi) worksetNode.node(), block);
			}
		}

		Set<WorksetNode> startWorkset = starters.stream()
			.sorted(Comparator.comparing(WorksetNode::distance))
			.limit(X86Register.registerCount())
			.collect(Collectors.toSet());

		// Spill phis we did not take
		for (WorksetNode starter : starters) {
			if (startWorkset.contains(starter) || !(starter.node() instanceof IkeaPhi)) {
				continue;
			}
			addPhiSpill((IkeaPhi) starter.node(), block);
		}

		// Mark nodes as spilled if they are spilled in a parent block. This ensures our fix block helper properly
		// cleans it up and spills it in the other parents as well.
		for (WorksetNode node : startWorkset) {
			// The value is from our block. This is not possible for a normal starter we inherited from a parent.
			// We are in the loop head and encountered a back edge to ourself. This does not count as spilled, we will
			// clean that up when processing this block.
			if (node.node().getBlock().equals(block)) {
				node.setSpilled(false);
				continue;
			}

			PredecessorAvailability availability = computePredecessorAvailability(block, node.node());
			if (availability == PredecessorAvailability.MIXED ||
				availability == PredecessorAvailability.SPILLED_IN_ALL) {
				node.setSpilled(true);
			}
		}

		return startWorkset;
	}

	private WorksetNode identityCrisis(IkeaBløck block, IkeaNode node, PredecessorAvailability availability) {
		WorksetNode worksetNode = new WorksetNode(node);

		Optional<Uses.NextUse> nextUse = uses.nextUse(liveliness, loopTree, node, block.nodes().get(0), false);
		// No use found? What exactly caused this?
		if (nextUse.isEmpty()) {
			LOGGER.debug("No use found for %s from block %s", node, block);
			worksetNode.setDistance(new Infinity());
			return worksetNode;
		}
		int nextUseTime = nextUse.get().distance();
		worksetNode.setDistance(new Distance(nextUseTime));

		// We do not want to take you if you are spilled in every single predecessor
		if (availability == PredecessorAvailability.SPILLED_IN_ALL) {
			LOGGER.debug("Not taking %s as it is spilled in all preds", node);
			worksetNode.setDistance(new Infinity());
			return worksetNode;
		}
		// Sounds good to me :)
		if (availability == PredecessorAvailability.LIVE_IN_ALL) {
			LOGGER.debug("Taking %s as it is live in all preds", node);
			return worksetNode;
		}

		int blockLoopDepth = loopTree.getBlockElement(block.origin()).depth();
		int candidateLoopDepth = nextUse.get().outermostLoopDepth();
		boolean isFurtherNestedInLoopTree = candidateLoopDepth > blockLoopDepth;
		// It's even deeper!
		if (isFurtherNestedInLoopTree) {
			LOGGER.debug("Taking %s as it is deeper nested (%s vs %s)", node, blockLoopDepth, candidateLoopDepth);
			return worksetNode;
		}

		LOGGER.debug("Delaying node %s as it is less nested (%s vs %s)", node, blockLoopDepth, candidateLoopDepth);
		worksetNode.setDistance(new LoopDelayed());

		return worksetNode;
	}

	private PredecessorAvailability computePredecessorAvailability(IkeaBløck block, IkeaNode node) {
		PredecessorAvailability result = PredecessorAvailability.MIXED;

		for (IkeaBløck inputBlock : controlFlow.inputBlocks(block)) {
			IkeaNode nodeToCheck = node;
			if (nodeToCheck instanceof IkeaPhi phi) {
				nodeToCheck = phi.getParents().get(inputBlock);
			}

			Set<WorksetNode> parentEndWorkset = endWorksets.get(inputBlock);

			if (parentEndWorkset == null || endWorksets.isEmpty()) {
				return PredecessorAvailability.UNKNOWN;
			}

			// If we find it is live, we mark it as LIVE_IN_ALL and then let the combine function figure out the rest
			PredecessorAvailability availability = PredecessorAvailability.SPILLED_IN_ALL;
			for (WorksetNode worksetNode : parentEndWorkset) {
				if (worksetNode.node().equals(nodeToCheck)) {
					availability = PredecessorAvailability.LIVE_IN_ALL;
					break;
				}
			}
			result = result.combineWith(availability);
		}

		return result;
	}

	/**
	 * Our {@link #processBlock(IkeaBløck)} is block-local and might cause the start/end workset of blocks to not be
	 * consistent with their parents. This can e.g. happen if you have multiple parents and some subset had to be
	 * decided on.
	 *
	 * @param block the block to fix up
	 */
	private void fixBlockBorder(IkeaBløck block) {
		Set<WorksetNode> startWorkset = startWorksets.get(block);

		Set<IkeaBløck> inputBlocks = controlFlow.inputBlocks(block);
		for (IkeaBløck parentBlock : inputBlocks) {
			Set<WorksetNode> endWorkset = endWorksets.get(parentBlock);

			// Spill values that are in the end of the parent workset but not part of our start workset
			for (WorksetNode node : endWorkset) {
				// All is well, the node is live and we have it locally!
				if (startWorkset.contains(node)) {
					continue;
				}

				// Value is not live-in. Whatever, nothing to do here
				if (!liveliness.getLiveIn(block, parentBlock).contains(node.node())) {
					continue;
				}

				// Value is in parent end-workset but not in our start-workset. We might need to spill it

				// We do not need to spill it, it is already spilled
				if (node.spilled()) {
					continue;
				}

				addSpillOnEdge(node.node(), block, parentBlock);
			}

			// reload values that are in the start workset but spilled in a parent
			for (WorksetNode node : startWorkset) {
				IkeaNode ikeaNode = node.node();
				// For phis we need to have a close look at the relevant predecessor
				if (node.node() instanceof IkeaPhi phi) {
					ikeaNode = phi.getParents().get(parentBlock);
				}

				// We need to reload, it is not in a register in our parent
				if (!worksetContains(endWorkset, ikeaNode)) {
					addReloadOnEdge(node.node(), block, parentBlock);
				} else {
					WorksetNode parentNode = worksetGet(endWorkset, ikeaNode);

					// Spilled locally but not in the parent, we gotta fix that!
					if (!parentNode.spilled() && node.spilled()) {
						addSpillOnEdge(ikeaNode, block, parentBlock);
					}
				}
			}
		}
	}

	private boolean worksetContains(Set<WorksetNode> workset, IkeaNode node) {
		return workset.stream().anyMatch(it -> it.node().equals(node));
	}

	private WorksetNode worksetGet(Set<WorksetNode> workset, IkeaNode node) {
		return workset.stream()
			.filter(it -> it.node().equals(node))
			.findFirst()
			.orElseThrow(() -> new InternalCompilerException("Could not find node in workset"));
	}

	private void addSpillOnEdge(IkeaNode node, IkeaBløck block, IkeaBløck parent) {
		// We have only one parent, we can spill at the entry to our block
		if (controlFlow.inputBlocks(block).size() == 1) {
			addSpill(node, block.nodes().get(0));
			return;
		}

		// We have more than one parent, so we need to move the spill to the parent block
		addSpill(node, parent.nodes().get(parent.nodes().size() - 1));
	}

	private void addReloadOnEdge(IkeaNode node, IkeaBløck block, IkeaBløck parent) {
		// We have only one parent, we can reload at the entry to our block
		if (controlFlow.inputBlocks(block).size() == 1) {
			addReload(node, block.nodes().get(0));
			return;
		}

		// We have more than one parent, so we need to move the reload to the parent block
		addReload(node, parent.nodes().get(parent.nodes().size() - 1));
	}

	private void addPhiSpill(IkeaPhi phi, IkeaBløck block) {
		for (var entry : phi.getParents().entrySet()) {
			addSpillOnEdge(entry.getValue(), block, entry.getKey());
		}
	}

	private void realizeSpillsAndReloads() {
		// TODO: We have spilled some whole phis but we do not really care, do we? We spilled the arguments as well
		// and do not really need anything else?

		// TODO: We could connect reloads with spills and spills with reloads. This might make detecting them easier
		//  later on, but also forces us to fix SSA form twice

		for (SpillInfo info : spillInfos.values()) {
			// TODO: Calculate spill costs and maybe do rematerialization

			spill(info);

			for (IkeaNode reloadBefore : info.reloadBefore()) {
				int insertionPoint = reloadBefore.getBlock().nodes().indexOf(reloadBefore) - 1;
				IkeaReload reload = new IkeaReload(null, reloadBefore.getBlock(), info.valueToSpill());
				reloadBefore.getBlock().nodes().add(insertionPoint, reload);
			}

			if (!info.reloadBefore().isEmpty()) {
				// Dominance and uses are *invalid* after this point but that's okay, our spilled values are
				// independent
				new SsaReconstruction(dominance, uses).ssaReconstruction(Set.of(info.valueToSpill()));
			}
		}

		// We need to fix up uses, dominance, liveliness and so forth
		dominance.recompute();
		uses.recompute();
		liveliness.recompute();
	}

	private void spill(SpillInfo spillInfo) {
		// No need to spill things twice
		if (spillInfo.spilled()) {
			return;
		}
		spillInfo.setSpilled(true);

		if (spillInfo.isPhi()) {
			spillPhi(spillInfo);
		} else {
			spillNode(spillInfo);
		}
	}

	private void spillNode(SpillInfo spillInfo) {
		for (IkeaNode spillAfter : spillInfo.toSpillAfter()) {
			IkeaSpill spill = new IkeaSpill(null, spillAfter.getBlock(), spillInfo.valueToSpill());

			int insertPoint = spillAfter.getBlock().nodes().indexOf(spillAfter) + 1;
			spillAfter.getBlock().nodes().add(insertPoint, spill);
		}
	}

	private void spillPhi(SpillInfo spillInfo) {
		// TODO: We don't need any replacement (memory) Phi here as we never reorder or optimize the order afterwards,
		//  right? If spill slots are unique that should work. A spill might happen in a block far above us but that
		//  should be fine as long as we use the same spillslot for it.

		IkeaPhi phi = (IkeaPhi) spillInfo.valueToSpill();
		for (IkeaNode node : phi.getParents().values()) {
			spill(spillInfos.get(node));
		}
	}

	private void assignSpillSlots() {
		Map<IkeaNode, Integer> slotIndices = new HashMap<>();
		for (IkeaBløck block : controlFlow.getAllBlocks()) {
			for (IkeaNode node : block.nodes()) {
				if (node instanceof IkeaReload reload) {
					int index = slotIndices.computeIfAbsent(node, ignored -> slotIndices.size());
					reload.setSpillSlot(index);
				} else if (node instanceof IkeaSpill spill) {
					int index = slotIndices.computeIfAbsent(node, ignored -> slotIndices.size());
					spill.setSpillSlot(index);
				}
			}
		}
	}

	private enum PredecessorAvailability {
		LIVE_IN_ALL,
		SPILLED_IN_ALL,
		MIXED,
		UNKNOWN;

		public PredecessorAvailability combineWith(PredecessorAvailability other) {
			if (this == UNKNOWN || other == UNKNOWN) {
				return UNKNOWN;
			}
			if (this == MIXED) {
				return MIXED;
			}
			if (this == other) {
				return this;
			}
			return MIXED;
		}
	}

	private static final class SpillInfo {
		private boolean spilled;
		private final IkeaNode valueToSpill;
		private final Set<IkeaNode> reloadBefore;
		private final Set<IkeaNode> toSpillAfter;

		private SpillInfo(IkeaNode valueToSpill, Set<IkeaNode> reloadBefore, Set<IkeaNode> toSpillAfter) {
			this.valueToSpill = valueToSpill;
			this.reloadBefore = reloadBefore;
			this.toSpillAfter = toSpillAfter;
		}

		public boolean isPhi() {
			return valueToSpill instanceof IkeaPhi;
		}

		public static SpillInfo forNode(IkeaNode node) {
			return new SpillInfo(node, new HashSet<>(), new HashSet<>());
		}

		public boolean spilled() {
			return spilled;
		}

		public void setSpilled(boolean spilled) {
			this.spilled = spilled;
		}

		public IkeaNode valueToSpill() {
			return valueToSpill;
		}

		public Set<IkeaNode> reloadBefore() {
			return reloadBefore;
		}

		public Set<IkeaNode> toSpillAfter() {
			return toSpillAfter;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			}
			var that = (SpillInfo) obj;
			return Objects.equals(this.valueToSpill, that.valueToSpill) &&
				Objects.equals(this.reloadBefore, that.reloadBefore) &&
				Objects.equals(this.toSpillAfter, that.toSpillAfter);
		}

		@Override
		public int hashCode() {
			return Objects.hash(valueToSpill, reloadBefore, toSpillAfter);
		}

		@Override
		public String toString() {
			return "SpillInfo[" + "valueToSpill=" + valueToSpill + ", " + "reloadBefore=" + reloadBefore + ", " +
				"toSpillAfter=" + toSpillAfter + ']';
		}

	}

	private static class WorksetNode {
		private final IkeaNode node;
		private boolean spilled;
		private NodeDistance distance;

		public WorksetNode(IkeaNode node) {
			this.node = node;
			this.distance = new UnknownDist();
		}

		public void setSpilled(boolean spilled) {
			this.spilled = spilled;
		}

		public boolean spilled() {
			return spilled;
		}

		public NodeDistance distance() {
			return distance;
		}

		public void setDistance(NodeDistance distance) {
			this.distance = distance;
		}

		public IkeaNode node() {
			return node;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			WorksetNode that = (WorksetNode) o;
			return Objects.equals(node, that.node);
		}

		@Override
		public int hashCode() {
			return Objects.hash(node);
		}

		@Override
		public String toString() {
			return "WorksetNode{" + "node=" + node + ", spilled=" + spilled + ", distance=" + distance + '}';
		}
	}

	private sealed interface NodeDistance extends Comparable<NodeDistance>
		permits Infinity, Distance, LoopDelayed, UnknownDist {

		int asInt();

		@Override
		default int compareTo(Belady.NodeDistance o) {
			return Integer.compare(asInt(), o.asInt());
		}
	}

	private record Infinity() implements NodeDistance {
		@Override
		public int asInt() {
			return Integer.MAX_VALUE;
		}
	}

	private record LoopDelayed() implements NodeDistance {
		@Override
		public int asInt() {
			return Integer.MAX_VALUE - 1;
		}
	}

	private record Distance(int distance) implements NodeDistance {
		@Override
		public int asInt() {
			return distance;
		}
	}

	private record UnknownDist() implements NodeDistance {
		@Override
		public int asInt() {
			return Integer.MAX_VALUE;
		}
	}
}
