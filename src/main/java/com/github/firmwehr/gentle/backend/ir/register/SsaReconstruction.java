package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;
import com.github.firmwehr.gentle.util.Mut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SsaReconstruction {

	private final Dominance dominance;
	private final Uses uses;
	private final Set<IkeaNode> definitions;

	public SsaReconstruction(Dominance dominance, Uses uses) {
		this.dominance = dominance;
		this.uses = uses;

		this.definitions = new HashSet<>();
	}

	public SsaReconstruction addDef(IkeaNode copy) {
		definitions.add(copy);
		return this;
	}

	/**
	 * Setting: The graph might contain multiply definitions for a variable (everything that was spilled was reloaded,
	 * resulting in multiple definitions). This method will take a list of variables with multiple definitions and
	 * rewire uses to refer to the closest correct definition (either a reload-node or a normal def).
	 * <p>
	 * No use should be fixed before this method is called as the {@link Uses} information is queried. This method will
	 * invalidate the state in {@link Uses} and it should be recomputed.
	 *
	 * @param brokenVariable all variables that now have multiple definitions
	 */
	public void ssaReconstruction(IkeaNode brokenVariable) {
		Set<IkeaBløck> F = new HashSet<>(dominance.getDominanceFrontier(brokenVariable.block()));

		Set<IkeaNode> uses = this.uses.uses(brokenVariable);

		for (IkeaNode use : uses) {
			for (int i = 0; i < use.inputs().size(); i++) {
				if (!use.inputs().get(i).equals(brokenVariable)) {
					continue;
				}
				IkeaNode x = findDef(use, use.inputs().get(i).block(), Set.of(brokenVariable), F);
				use.graph().setInput(use, i, x);
			}
		}
	}

	private IkeaNode findDef(IkeaNode use, IkeaBløck parent, Set<IkeaNode> brokenVariables, Set<IkeaBløck> F) {
		if (use instanceof IkeaPhi phi) {
			use = phi.parent(parent);
		}
		while (true) {
			// Try to find last def before use in use block
			List<IkeaNode> nodes = use.block().nodes();
			for (int i = nodes.indexOf(use); i >= 0; i--) {
				IkeaNode node = nodes.get(i);
				if (brokenVariables.contains(node)) {
					return node;
				}
				if (definitions.contains(node)) {
					return node;
				}
			}

			// No direct def found, and we are in a frontier block: insert phi
			if (F.contains(use.block())) {
				IkeaPhi phi =
					new IkeaPhi(new Mut<>(Optional.empty()), use.block(), use.graph(), List.of(),
						use.graph().nextId());
				use.block().nodes().add(0, phi);
				List<IkeaNode> phiParents = new ArrayList<>();

				// Fill the slots by finding the last definition in the relevant parent blocks
				for (IkeaParentBløck phiParent : phi.block().parents()) {
					List<IkeaNode> parentNodes = phiParent.parent().nodes();
					IkeaNode lastInParent = parentNodes.get(parentNodes.size() - 1);
					phiParents.add(findDef(lastInParent, phiParent.parent(), brokenVariables, F));
				}

				phi.graph().addNode(phi, phiParents);
				return phi;
			}

			// no def found in this block and not a frontier: Check for def in immediate dominator
			// We can skip everything between us and the idom as we are not a frontier block.
			// Any definition we can find must dominate this use
			Optional<IkeaBløck> idom = dominance.getIdom(use.block());
			if (idom.isEmpty()) {
				throw new InternalCompilerException("No def found");
			}
			use = idom.get().nodes().get(idom.get().nodes().size() - 1);
		}
	}
}
