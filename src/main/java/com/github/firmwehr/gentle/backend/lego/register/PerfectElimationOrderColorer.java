package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
import com.github.firmwehr.gentle.output.Logger;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tries to color our graph using a perfect elimination order. We pray it exists. It should, if we did not fuck up our
 * SSA form in the meantime.
 */
public class PerfectElimationOrderColorer {

	private static final Logger LOGGER = new Logger(PerfectElimationOrderColorer.class);

	private final ControlFlowGraph graph;
	private final LifetimeAnalysis liveliness;
	private final Dominance dominance;
	private final Uses uses;

	public PerfectElimationOrderColorer(
		ControlFlowGraph graph, LifetimeAnalysis liveliness, Dominance dominance, Uses uses
	) {
		this.graph = graph;
		this.liveliness = liveliness;
		this.dominance = dominance;
		this.uses = uses;
	}

	public void colorGraph() {
		colorRecursive(graph.getStart());
	}

	private void colorRecursive(LegoPlate block) {
		Set<X86Register> assigned = EnumSet.noneOf(X86Register.class);

		// Live-in is already colored
		for (LegoNode node : liveliness.getAllLiveIn(block)) {
			if (node.register().isPresent()) {
				assigned.add(node.uncheckedRegister());
				LOGGER.debug("Keeping assigned register %s for live in %s", node.uncheckedRegister(), node);
				continue;
			}
			// Phis introduce loops but I ~~think~~ *pray* this might work.
			if (node.graph()
				.getOutputs(node)
				.stream()
				.filter(it -> it.block().equals(block))
				.anyMatch(it -> it instanceof LegoPhi)) {
				continue;
			}
			throw new InternalCompilerException("Expected to have a register for " + node);
		}

		for (LegoNode node : block.nodes()) {
			for (LegoNode parent : node.inputs()) {
				// Can free it now, it was the last use - the value is dead now, jim
				if (parent.register().isPresent() && uses.isLastUse(liveliness, parent, node)) {
					assigned.remove(parent.uncheckedRegister());
					LOGGER.debug("Freeing register %s from %s as it was last use", parent.uncheckedRegister(), parent);
				}
			}

			LOGGER.debug("At %s assigned: %s", node, assigned);
			if (node.registerIgnore()) {
				continue;
			}
			if (node.register().isEmpty()) {
				node.register(freeRegister(assigned, node));
				assigned.add(node.uncheckedRegister());
				LOGGER.debug("Assigning register %s to %s", node.uncheckedRegister(), node);
			} else {
				assigned.add(node.register().get());
				LOGGER.debug("Keeping assigned register %s for %s", node.uncheckedRegister(), node);
			}

			if (node.graph().getOutputs(node).isEmpty()) {
				assigned.remove(node.uncheckedRegister());
				LOGGER.debug("Freeing register %s for %s as it was never used", node.uncheckedRegister(), node);
			}
		}

		for (LegoPlate dominatedBlock : dominance.getDirectlyDominatedBlocks(block)) {
			colorRecursive(dominatedBlock);
		}
	}

	private X86Register freeRegister(Set<X86Register> assigned, LegoNode node) {
		Set<X86Register> free = X86Register.all();
		free.removeAll(assigned);
		free.retainAll(node.registerRequirement().limitedTo());
		return free.iterator().next();
	}
}
