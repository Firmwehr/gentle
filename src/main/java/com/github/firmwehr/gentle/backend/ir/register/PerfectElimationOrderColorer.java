package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;

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

	public void colorGraph() {
		colorRecursive(graph.getStart());
	}

	private void colorRecursive(IkeaBløck block) {
		Set<X86Register> assigned = EnumSet.noneOf(X86Register.class);

		// Live-in is already colored
		for (IkeaNode node : liveliness.getAllLiveIn(block)) {
			if (node.register().isPresent()) {
				assigned.add(node.uncheckedRegister());
				continue;
			}
			// Phis introduce loops but I ~~think~~ *pray* this might work.
			if (node.graph()
				.getOutputs(node)
				.stream()
				.filter(it -> it.block().equals(block))
				.anyMatch(it -> it instanceof IkeaPhi)) {
				continue;
			}
			throw new InternalCompilerException("Expected to have a register for " + node);
		}

		for (IkeaNode node : block.nodes()) {
			for (IkeaNode parent : node.inputs()) {
				// Can free it now, it was the last use - the value is dead now, jim
				if (parent.register().isPresent() && uses.isLastUse(liveliness, parent, node)) {
					assigned.remove(parent.uncheckedRegister());
					assigned.removeAll(parent.clobbered());
				}
			}

			if (node.registerIgnore()) {
				continue;
			}
			if (node.register().isEmpty()) {
				node.register(freeRegister(assigned, node));
				assigned.add(node.uncheckedRegister());
				assigned.addAll(node.clobbered());
			} else {
				assigned.add(node.register().get());
			}

			if (uses.isLastUse(liveliness, node, node)) {
				assigned.remove(node.uncheckedRegister());
				assigned.removeAll(node.clobbered());
			}
		}

		for (IkeaBløck dominatedBlock : dominance.getDirectlyDominatedBlocks(block)) {
			colorRecursive(dominatedBlock);
		}
	}

	private X86Register freeRegister(Set<X86Register> assigned, IkeaNode node) {
		Set<X86Register> free = X86Register.all();
		free.removeAll(assigned);
		free.retainAll(node.registerRequirement().limitedTo());
		// TODO: The use of IkeaAdd (43) in printFromBuf should have been rewired by ssa reconstruction before
		// IkeaCall(46)
		return free.iterator().next();
	}
}
