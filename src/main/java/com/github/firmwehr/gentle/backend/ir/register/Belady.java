package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.output.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Belady {

	private static final Logger LOGGER = new Logger(Belady.class, Logger.LogLevel.DEBUG);

	private final Map<IkeaBløck, Set<IkeaNode>> startWorksets;
	private final Map<IkeaBløck, Set<IkeaNode>> endWorksets;

	public Belady() {
		this.startWorksets = new HashMap<>();
		this.endWorksets = new HashMap<>();
	}

	public void spill(ControlFlowGraph graph) {
		for (IkeaBløck block : graph.reversePostOrder()) {
			processBlock(block);
		}

		// TODO: Fix block borders!
	}

	private void processBlock(IkeaBløck block) {
		if (startWorksets.containsKey(block)) {
			throw new InternalCompilerException("Visited a block twice?");
		}
		startWorksets.put(block, decideStartWorkset(block));

		LOGGER.debugHeader("Start workset for %s", block.id());
		LOGGER.debug("  %s", startWorksets.get(block));

		// This will become out end workset
		Set<IkeaNode> currentBlockWorkset = new HashSet<>(startWorksets.get(block));

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
		Collection<IkeaNode> newValues, Set<IkeaNode> currentWorkset, IkeaNode currentInstruction, boolean isUsage
	) {
		int additionalPressure = 1; // We do not use all registers (sp is not a good idea?)

		int demand = newValues.size();
		for (IkeaNode value : newValues) {
			// Needs a reload!
			if (!currentWorkset.contains(value)) {
				// add a reload here!
				LOGGER.debug("Adding reload for %s before %s", value, currentInstruction);
				addReload(value, currentInstruction);
			} else {
				// Remove so it is not accidentally selected for spilling
				currentWorkset.remove(value);
				LOGGER.debug("%s was already live before %s", value, currentInstruction);
			}
		}

		demand += additionalPressure;

		// e.g. 10 Registers in X86, demand ist 4 and size is 4
		// 8 - 10 => -2 => No spills needed
		int neededSpills = demand + currentWorkset.size() - X86Register.registerCount();

		if (neededSpills > 0) {
			LOGGER.debug("Need to make room for %s values", neededSpills);

			// Dumb heuristic for now, just spill a few random ones
			for (int i = 0; i < neededSpills; i++) {
				IkeaNode victim = currentWorkset.iterator().next();
				currentWorkset.remove(victim);
				LOGGER.debug("Spilling %s before %s", victim, currentInstruction);
				IkeaNode victimParent = victim.getBlock().nodes().get(victim.getBlock().nodes().indexOf(victim) - 1);
				// TODO: Check if dead or already spilled
				addSpill(victim, victimParent);
			}
		}

		currentWorkset.addAll(newValues);
	}

	private void addReload(IkeaNode valueToReload, IkeaNode before) {
		// TODO
		throw new IllegalStateException("TODO");
	}

	private void addSpill(IkeaNode valueToSpill, IkeaNode after) {
		// Remove all spills we dominate, only add spill if we are not dominated
		// TODO
		throw new IllegalStateException("TODO");
	}

	private Set<IkeaNode> decideStartWorkset(IkeaBløck block) {
		if (block.parents().isEmpty()) {
			return new HashSet<>();
		}
		if (block.parents().size() == 1) {
			return new HashSet<>(endWorksets.get(block.parents().get(0).parent()));
		}

		// TODO: More magic!
		return new HashSet<>();
	}
}
