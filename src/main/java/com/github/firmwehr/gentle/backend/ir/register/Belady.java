package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaReload;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaSpill;
import com.github.firmwehr.gentle.output.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Belady {

	private static final Logger LOGGER = new Logger(Belady.class, Logger.LogLevel.DEBUG);

	private final Map<IkeaBløck, Set<WorksetNode>> startWorksets;
	private final Map<IkeaBløck, Set<WorksetNode>> endWorksets;
	private final Map<IkeaNode, SpillInfo> spillInfos;
	private final Dominance dominance;
	private final ControlFlowGraph controlFlow;
	private final LifetimeAnalysis liveliness;
	private final Uses uses;

	public Belady(Dominance dominance, ControlFlowGraph controlFlow, LifetimeAnalysis liveliness, Uses uses) {
		this.dominance = dominance;
		this.controlFlow = controlFlow;
		this.liveliness = liveliness;
		this.uses = uses;

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

		// TODO: Spill slot assignment
		// TODO: Spill slot coalescing
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
			toInsert.add(worksetValue);
		}

		demand += additionalPressure;

		// e.g. 10 Registers in X86, demand ist 4 and size is 4
		// 8 - 10 => -2 => No spills needed
		int neededSpills = demand + currentWorkset.size() - X86Register.registerCount();

		if (neededSpills > 0) {
			LOGGER.debug("Need to make room for %s values", neededSpills);

			// Dumb heuristic for now, just spill a few random ones
			for (int i = 0; i < neededSpills; i++) {
				WorksetNode victim = currentWorkset.iterator().next();
				IkeaNode victimNode = victim.node();
				currentWorkset.remove(victim);
				LOGGER.debug("Spilling %s before %s", victim, currentInstruction);
				IkeaNode victimParent =
					victimNode.getBlock().nodes().get(victimNode.getBlock().nodes().indexOf(victimNode) - 1);
				// TODO: Check if dead or already spilled
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

		// TODO: More magic!
		return new HashSet<>();
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

	private void realizeSpillsAndReloads() {
		// TODO: Spill whole phis first! Do not insert spilled phis in spill info but do insert their arguments

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
		if (spillInfo.spilled) {
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

		public WorksetNode(IkeaNode node) {
			this.node = node;
		}

		public void setSpilled(boolean spilled) {
			this.spilled = spilled;
		}

		public boolean spilled() {
			return spilled;
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
	}


}
