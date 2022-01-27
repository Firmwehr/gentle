package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPerm;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Mut;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
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

		// TODO: Pair up perm args that die at node with node defs => They can use the same register. Use intersection
		//  of allowed registers as requirements.

		// Order is unchanged, so this is fine
		for (BipartiteEntry entry : BipartiteSolver.forNodes(toPerm).solve()) {
			// The matching should be perfect as we have only one constrained instruction in our subgraph and no
			// instruction can use the same register twice as input
			X86Register register = entry.assignedRegister()
				.orElseThrow(() -> new InternalCompilerException("No out register assigned for " + entry));
			perm.getOutRegisters().add(register);
		}

		// We might have screwed these things over royally
		uses.recompute();
		liveliness.recompute();
		dominance.recompute();
	}

	private boolean isConstrained(IkeaNode node) {
		return node.inRequirements().stream().anyMatch(IkeaRegisterRequirement::limited);
	}

	/**
	 * Computes a perfect bipartite matching from nodes to registers.
	 */
	private static class BipartiteSolver {
		private static final Logger LOGGER = new Logger(BipartiteSolver.class);

		private final List<BipartiteEntry> entries;
		private final Set<X86Register> freeRegisters;

		private BipartiteSolver(List<BipartiteEntry> entries, Set<X86Register> freeRegisters) {
			this.entries = new ArrayList<>(entries);
			this.freeRegisters = EnumSet.copyOf(freeRegisters);
		}

		/**
		 * Solves the matching problem. The nodes are returned in the same order they were added.
		 *
		 * @return the resulting entries
		 */
		public List<BipartiteEntry> solve() {
			while (iteration()) {
				LOGGER.debugHeader("Change detected, running next bipartite iteration");
			}
			return entries;
		}

		private boolean iteration() {
			Mut<Boolean> changed = new Mut<>(false);

			for (BipartiteEntry entry : entries) {
				// Already assigned one, maybe we can improve it though
				if (entry.assignedRegister().isPresent()) {
					// Try to solve the following:
					//  Entry -> Register
					//    1   ->  {A, B, C}
					//    2   ->  {B}
					//    3   ->  {C}
					//
					// Current assignment:
					//    1 -> B
					//    2 -> ?
					//    3 -> C
					//
					// This is not perfect as 2 is unmatched and its register is no longer free. We notice though, that
					// 1 has an unmatched neighbour: It could be assigned to A, but it is not. As A is still free, the
					// slot probably is needed by another entry (2 here) that has stricter limits.
					// To solve this we try to find an unmatched entry that we can assign to the current register of 1
					// and move 1 to the first free neighbour we found.
					// This results in the following permutation:
					//   1 -> A     | was 1 -> B
					//   2 -> B     | was 2 -> ?
					//
					// And this results in the assignment
					//   1 -> A
					//   2 -> B
					//   3 -> C
					//
					// which is optimal enough.

					// All neighbours are already matched, no chance to improve this :)
					if (entry.allowedRegisters().stream().noneMatch(freeRegisters::contains)) {
						continue;
					}

					X86Register oldRegister = entry.assignedRegister().get();
					// exists as we checked for a free register above
					X86Register newRegister = findRegisterFor(entry).orElseThrow();

					Optional<BipartiteEntry> potentialLeft = entries.stream()
						.filter(it -> it.assignedRegister().isEmpty())
						.filter(it -> it.allowedRegisters().contains(oldRegister))
						.findAny();
					// No node found that could occupy our right side instead of us
					if (potentialLeft.isEmpty()) {
						continue;
					}

					BipartiteEntry newLeft = potentialLeft.get();
					LOGGER.debug("Swapping neighbour for %s and %s", entry, newLeft);
					assignRegister(newLeft, oldRegister);
					assignRegister(entry, newRegister);
					LOGGER.debug("  New assignment: %s, %s", entry, newLeft);
					changed.set(true);
				} else {
					LOGGER.debug("Trying to find free register for %s", entry);
					// Try to find a free register
					findRegisterFor(entry).ifPresent(register -> {
						LOGGER.debug("  Found %s", register);
						assignRegister(entry, register);
						changed.set(true);
					});
				}
			}

			return changed.get();
		}

		private Optional<X86Register> findRegisterFor(BipartiteEntry entry) {
			for (X86Register register : freeRegisters) {
				if (entry.allowedRegisters().contains(register)) {
					return Optional.ofNullable(register);
				}
			}
			return Optional.empty();
		}

		private void assignRegister(BipartiteEntry entry, X86Register register) {
			entry.setAssignedRegister(register);
			freeRegisters.remove(register);
		}

		/**
		 * Constructs a solver instance for the passed nodes.
		 *
		 * @param nodes the input nodes to match to registers
		 *
		 * @return the created solver instance
		 */
		public static BipartiteSolver forNodes(Collection<IkeaNode> nodes) {
			List<BipartiteEntry> entries = nodes.stream().map(BipartiteEntry::forNode).toList();

			Set<X86Register> freeRegisters = X86Register.all();
			for (BipartiteEntry entry : entries) {
				entry.assignedRegister().ifPresent(freeRegisters::remove);
			}

			return new BipartiteSolver(entries, freeRegisters);
		}
	}

	public static class BipartiteEntry {
		private final Set<X86Register> allowedRegisters;
		private X86Register assignedRegister;

		private BipartiteEntry(Set<X86Register> allowedRegisters, X86Register assignedRegister) {
			this.allowedRegisters = allowedRegisters;
			this.assignedRegister = assignedRegister;
		}

		private static BipartiteEntry forNode(IkeaNode node) {
			X86Register assignedRegister =
				node.regRequirement().limited() ? node.regRequirement().limitedTo().iterator().next() : null;
			return new BipartiteEntry(node.regRequirement().limitedTo(), assignedRegister);
		}

		public Set<X86Register> allowedRegisters() {
			return allowedRegisters;
		}

		public Optional<X86Register> assignedRegister() {
			return Optional.ofNullable(assignedRegister);
		}

		public void setAssignedRegister(X86Register register) {
			assignedRegister = register;
		}

		@Override
		public String toString() {
			return "BipartiteEntry{" + "allowedRegisters=" +
				(allowedRegisters.size() == X86Register.registerCount() ? "all" : allowedRegisters) +
				", assignedRegister=" + assignedRegister + '}';
		}
	}
}
