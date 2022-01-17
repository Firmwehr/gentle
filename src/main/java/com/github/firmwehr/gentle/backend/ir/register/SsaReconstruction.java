package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaVirtualRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPhi;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class SsaReconstruction {

	private final Dominance dominance;
	private final Uses uses;

	public SsaReconstruction(Dominance dominance, Uses uses) {
		this.dominance = dominance;
		this.uses = uses;
	}

	/**
	 * Setting: The graph might contain multiply definitions for a variable (everything that was spilled was reloaded,
	 * resulting in multiple definitions). This method will take a list of variables with multiple definitions and
	 * rewire uses to refer to the closest correct definition (either a reload-node or a normal def).
	 * <p>
	 * No use should be fixed before this method is called as the {@link Uses} information is queried. This method will
	 * invalidate the state in {@link Uses} and it should be recomputed.
	 *
	 * @param brokenVariables all variables that now have multiple definitions
	 */
	public void ssaReconstruction(Set<IkeaNode> brokenVariables) {
		Set<IkeaBløck> F = brokenVariables.stream()
			.map(it -> dominance.getDominanceFrontier(it.getBlock()))
			.flatMap(Collection::stream)
			.collect(Collectors.toSet());

		Set<IkeaNode> uses =
			brokenVariables.stream().flatMap(it -> this.uses.uses(it).stream()).collect(Collectors.toSet());

		for (IkeaNode use : uses) {
			for (int i = 0; i < use.parents().size(); i++) {
				IkeaNode x = findDef(use, use.parents().get(i).getBlock(), brokenVariables, F);
				use.parents().set(i, x);
			}
		}
	}

	private IkeaNode findDef(IkeaNode use, IkeaBløck parent, Set<IkeaNode> brokenVariables, Set<IkeaBløck> F) {
		if (use instanceof IkeaPhi phi) {
			use = phi.getParents().get(parent);
		}
		while (true) {
			// Try to find last def before use in use block
			List<IkeaNode> nodes = use.getBlock().nodes();
			for (int i = nodes.indexOf(use); i >= 0; i--) {
				// TODO: Also check for reloads for this var
				if (brokenVariables.contains(nodes.get(i))) {
					return nodes.get(i);
				}
			}

			// No direct def found, and we are in a frontier block: insert phi
			if (F.contains(use.getBlock())) {
				// We always need one, I think
				IkeaVirtualRegister box =
					new IkeaVirtualRegister(ThreadLocalRandom.current().nextInt(400, 1000), use.box().size());
				IkeaPhi phi = new IkeaPhi(box, null, use.getBlock());
				// TODO: insert properly
				use.getBlock().nodes().add(0, phi);

				// Fill the slots by finding the last definition in the relevant parent blocks
				for (IkeaParentBløck phiParent : phi.getBlock().parents()) {
					List<IkeaNode> parentNodes = phiParent.parent().nodes();
					IkeaNode lastInParent = parentNodes.get(parentNodes.size() - 1);
					phi.addParent(findDef(lastInParent, phiParent.parent(), brokenVariables, F), phiParent.parent());
				}
				return phi;
			}

			// no def found in this block and not a frontier: Check for def in immediate dominator
			// We can skip everything between us and the idom as we are not a frontier block.
			// Any definition we can find must dominate this use
			Optional<IkeaBløck> idom = dominance.getIdom(use.getBlock());
			if (idom.isEmpty()) {
				throw new InternalCompilerException("No def found");
			}
			use = idom.get().nodes().get(idom.get().nodes().size() - 1);
		}
	}
}
