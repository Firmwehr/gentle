package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tries to color our graph using a perfect elimination order. We pray it exists. It should, if we did not fuck up our
 * SSA form in the meantime.
 */
public class PerfectElimationOrderColorer {

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

	public void colorProgram() {
		colorRecursive(graph.getStart());
	}

	private void colorRecursive(IkeaBløck block) {
		Set<X86Register> assigned = EnumSet.noneOf(X86Register.class);

		// Live-in is already colored
		for (IkeaNode node : liveliness.getAllLiveIn(block)) {
			assigned.add(getRegister(node));
		}

		for (IkeaNode node : block.nodes()) {
			for (IkeaNode parent : node.parents()) {
				// Can free it now, it was the last use - the value is dead now, jim
				if (uses.isLastUse(liveliness, node)) {
					assigned.remove(getRegister(parent));
				}
			}

			assignRegister(node, freeRegister(assigned));
			assigned.add(getRegister(node));
		}

		for (IkeaBløck dominatedBlock : dominance.getDirectlyDominatedBlocks(block)) {
			colorRecursive(dominatedBlock);
		}
	}

	private X86Register getRegister(IkeaNode node) {
		// TODO: Implement
		return null;
	}

	private void assignRegister(IkeaNode node, X86Register register) {
		// TODO: Implement
	}

	private X86Register freeRegister(Set<X86Register> assigned) {
		Set<X86Register> free = X86Register.all();
		free.removeAll(assigned);
		return free.iterator().next();
	}
}
