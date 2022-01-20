package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaCopy;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class Spillprepare {

	private static final Logger LOGGER = new Logger(Spillprepare.class);

	private final LifetimeAnalysis liveliness;
	private final Dominance dominance;
	private final Uses uses;

	public Spillprepare(LifetimeAnalysis liveliness, Dominance dominance, Uses uses) {
		this.liveliness = liveliness;
		this.dominance = dominance;
		this.uses = uses;
	}

	public void prepare(ControlFlowGraph graph) {
		insertMissingCopies(graph);
	}

	private void insertMissingCopies(ControlFlowGraph graph) {
		for (IkeaBløck block : graph.getAllBlocks()) {
			for (ListIterator<IkeaNode> iterator = block.nodes().listIterator(); iterator.hasNext(); ) {
				IkeaNode node = iterator.next();
				List<IkeaCopy> copies = new ArrayList<>();
				copies.addAll(getCopiesForMultipleConstrainedArguments(node));
				copies.addAll(getCopiesForDyingArguments(node));

				for (IkeaCopy copy : copies) {
					iterator.add(copy);
				}
			}
		}
	}

	private List<IkeaCopy> getCopiesForMultipleConstrainedArguments(IkeaNode node) {
		List<IkeaCopy> copies = new ArrayList<>();

		for (int i = 0; i < node.parents().size(); i++) {
			IkeaNode first = node.parents().get(i);

			if (!first.regRequirement().limited()) {
				continue;
			}

			for (int j = i + 1; j < node.parents().size(); j++) {
				IkeaNode second = node.parents().get(j);
				if (!first.equals(second)) {
					continue;
				}
				// Same node used in multiple places
				IkeaRegisterRequirement inReq = second.inRequirements().get(j);

				// We are limited to different registers so we need to copy!
				if (!inReq.limited() || inReq.limitedTo().equals(first.regRequirement().limitedTo())) {
					continue;
				}

				// TODO: Use SSA reconstruction code and use copy in rest!
				// FIXME: keep virtual register allocator
				IkeaCopy copy = new IkeaCopy(null, first, first.getBlock());
				copies.add(copy);
				first.parents().set(j, copy);
			}
		}

		return copies;
	}

	private List<IkeaCopy> getCopiesForDyingArguments(IkeaNode node) {
		List<IkeaCopy> copies = new ArrayList<>();

		Set<X86Register> outClobbered = node.clobbered();

		for (int i = 0; i < node.parents().size(); i++) {
			IkeaNode in = node.parents().get(i);
			if (!in.regRequirement().limited()) {
				continue;
			}
			boolean overlap = !Sets.intersection(outClobbered, in.regRequirement().limitedTo()).isEmpty();
			if (!overlap) {
				continue;
			}
			// Don't think I need to be careful here, but who knows...
			if (in instanceof IkeaCopy) {
				LOGGER.warn("Hello there %s, I found a copy in", in);
				continue;
			}

			if (!isLiveAfter(node, in)) {
				continue;
			}

			IkeaCopy copy = new IkeaCopy(null, in, node.getBlock());
			copies.add(copy);
			node.parents().set(i, copy);
		}

		return copies;
	}

	private boolean isLiveAfter(IkeaNode value, IkeaNode after) {
		IkeaBløck afterBlock = after.getBlock();
		int afterSchedule = afterBlock.nodes().indexOf(after);

		// Input does not dominate us => Can not be live here.
		if (!dominance.dominates(value, after)) {
			return false;
		}
		// Live out => Survives us for sure
		if (liveliness.getLiveOut(afterBlock).contains(value)) {
			return true;
		}
		// Any use is below us => Survives us!
		return uses.uses(value)
			.stream()
			.filter(it -> it.getBlock().equals(afterBlock))
			.map(it -> afterBlock.nodes().indexOf(it))
			.anyMatch(scheduleIndex -> scheduleIndex > afterSchedule);
	}
}
