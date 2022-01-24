package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPerm;

import java.util.List;
import java.util.Set;

public class ConstraintNodePrepare {

	private final LifetimeAnalysis liveliness;
	private final Uses uses;
	private final Dominance dominance;

	public ConstraintNodePrepare(LifetimeAnalysis liveliness, Uses uses, Dominance dominance) {
		this.liveliness = liveliness;
		this.uses = uses;
		this.dominance = dominance;
	}

	public void prepare(ControlFlowGraph controlFlowGraph) {
		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			for (IkeaNode node : block.nodes()) {
				if (isConstrained(node)) {
					addPermForNode(node);
				}
			}
		}
	}

	private void addPermForNode(IkeaNode node) {
		List<IkeaNode> toPerm = List.copyOf(liveliness.getLiveBefore(node));
		IkeaPerm perm = new IkeaPerm(toPerm, node.getBlock());
		int nodeIndex = node.getBlock().nodes().indexOf(node);
		node.getBlock().nodes().add(nodeIndex, perm);

		// Yay, we have a perm now. Congrats. This also breaks SSA:
		//             Head
		//           /     \
		//        Perm    Other stuff
		//           \     /
		//             Use
		// We need to introduce a phi for the use if our perm introduces a new definition
		new SsaReconstruction(dominance, uses).ssaReconstruction(Set.copyOf(toPerm));

		// Copy in requirements to perm. This ensures e.g. a call with a requirement of "EAX" for a register will have
		// that reflected in the out requirements of the perm. We can use this to compute a valid matching without
		// needing backedges
		perm.setOutRequirements(toPerm.stream().map(IkeaNode::inRequirements).toList());

		// We might have screwed these things over royally
		uses.recompute();
		liveliness.recompute();
		dominance.recompute();
	}

	private boolean isConstrained(IkeaNode node) {
		return node.inRequirements().stream().anyMatch(IkeaRegisterRequirement::limited);
	}
}
