package com.github.firmwehr.gentle.backend.lego.simplereg;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovRegister;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoReload;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoSpill;
import com.github.firmwehr.gentle.backend.lego.register.ControlFlowGraph;
import com.github.firmwehr.gentle.backend.lego.register.Dominance;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.register.LifetimeAnalysis;
import com.github.firmwehr.gentle.backend.lego.register.Spillprepare;
import com.github.firmwehr.gentle.backend.lego.register.Uses;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Pair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Scanny {

	private static final Logger LOGGER = new Logger(Scanny.class, Logger.LogLevel.DEBUG);

	private final ControlFlowGraph controlFlowGraph;
	private final Uses uses;
	private final LifetimeAnalysis liveliness;
	private final Dominance dominance;

	private final Set<LegoNode> liveNodes;
	private final Set<X86Register> freeRegisters;
	private final Map<LegoNode, SpillNode> spillNodes;
	private final SpillContext spillContext;
	private final Set<RewireCleanup> rewireCleanups;

	public Scanny(ControlFlowGraph controlFlowGraph, Uses uses, LifetimeAnalysis liveliness, Dominance dominance) {
		this.controlFlowGraph = controlFlowGraph;
		this.uses = uses;
		this.liveliness = liveliness;
		this.dominance = dominance;

		this.liveNodes = new HashSet<>();
		this.freeRegisters = EnumSet.allOf(X86Register.class);
		this.spillNodes = new HashMap<>();
		this.spillContext = new SpillContext(new HashMap<>(), dominance);
		this.rewireCleanups = new HashSet<>();
	}

	public void assignRegisters() {
		LOGGER.info("Fixing multiple-constrained arguments");
		// Ensure no value is used in multiple registers at once
		new Spillprepare(liveliness, dominance, uses).fixMultipleConstrainedArguments(controlFlowGraph);

		GraphDumper.dumpGraph(controlFlowGraph, "multiple-constrained");

		LOGGER.info("Handling blocks");
		for (LegoPlate block : controlFlowGraph.reversePostOrder()) {
			handleBlock(block);
		}

		LOGGER.info("Reloading and realizing");
		// Now we need to do spillslot handling
		for (LegoPlate block : controlFlowGraph.reversePostOrder()) {
			reloadAndSpillArgumentsAndResults(block);
			realizeForBlock(block);
		}

		GraphDumper.dumpGraph(controlFlowGraph, "ra-no-clobber");

		// realize inserts spills and reloads
		liveliness.recompute();
		dominance.recompute();

		LOGGER.info("Fixing clobbers");
		// Needs to be after insertion of the rest so live nodes can be accurately determined
		for (LegoPlate block : controlFlowGraph.reversePostOrder()) {
			fixClobbers(block);
			realizeForBlock(block);
		}

		for (RewireCleanup cleanup : rewireCleanups) {
			cleanup.source().graph().setInput(cleanup.source(), cleanup.index(), cleanup.newArgument());
		}

		assignSpillSlots();

		GraphDumper.dumpGraph(controlFlowGraph, "ra");

		// realize inserts spills and reloads
		liveliness.recompute();
		dominance.recompute();

	}

	private void realizeForBlock(LegoPlate block) {
		for (int i = 0; i < block.nodes().size(); ) {
			LegoNode legoNode = block.nodes().get(i);

			if (legoNode.registerIgnore() || legoNode instanceof LegoSpill || legoNode instanceof LegoReload ||
				legoNode instanceof LegoMovRegister) {
				i++;
				continue;
			}

			SpillNode node = spillNodes.get(legoNode);
			for (LegoSpill spill : node.spillsBefore()) {
				block.nodes().add(i, spill);
				spill.graph().addNode(spill, List.of(spill.originalValue()));
				i++;
			}
			for (var pair : node.movesBefore()) {
				var move = pair.first();
				block.nodes().add(i, move.second());
				move.second().graph().addNode(move.second(), List.of(move.first()));
				rewireCleanups.add(new RewireCleanup(node.lego(), pair.second(), move.second()));
				i++;
			}
			for (var reload : node.reloadsBefore()) {
				block.nodes().add(i, reload.first());
				reload.first().graph().addNode(reload.first(), List.of());
				rewireCleanups.add(new RewireCleanup(node.lego(), reload.second(), reload.first()));
				i++;
			}

			// Next is our node
			if (!legoNode.equals(node.lego())) {
				throw new InternalCompilerException("Weird insertion?");
			}
			i++;

			for (LegoSpill spill : node.spillsAfter()) {
				block.nodes().add(i, spill);
				spill.graph().addNode(spill, List.of(spill.originalValue()));
				i++;
			}
			for (LegoReload reload : node.reloadsAfter()) {
				block.nodes().add(i, reload);
				reload.graph().addNode(reload, List.of());
				i++;
			}

			node.clear();
		}
	}

	private void handleBlock(LegoPlate block) {
		for (LegoNode node : block.nodes()) {
			// Try to find free registers for inputs
			allocateInputs(node);

			// Clear out live nodes that are no longer needed
			for (LegoNode input : node.inputs()) {
				if (input.register().isPresent() && uses.isLastUse(liveliness, input, node)) {
					liveNodes.remove(input);
					freeRegisters.add(input.uncheckedRegister());
				}
			}

			if (!node.registerIgnore()) {
				allocateOutputs(node);
			} else {
				spillNodes.computeIfAbsent(node, it -> SpillNode.forNode(it, spillContext));
			}
		}
	}

	private void allocateInputs(LegoNode instruction) {
		List<LegoNode> inputs = instruction.inputs();
		for (int i = 0; i < inputs.size(); i++) {
			LegoNode input = inputs.get(i);

			// If we already have a spill node for this input it either has a register or should be on the stack
			if (spillNodes.containsKey(input)) {
				continue;
			}
			updateSpillNode(input, SpillNode.forNode(input, spillContext));

			if (input.registerIgnore()) {
				continue;
			}

			if (liveNodes.contains(input)) {
				continue;
			}
			if (freeRegisters.isEmpty()) {
				continue;
			}
			LegoRegisterRequirement requirement = instruction.inRequirements().get(i);
			if (!requirement.limited()) {
				input.register(allocateNextRegister());
				liveNodes.add(input);

				// Update created spillnode
				updateSpillNode(input, spillNodes.get(input).withRegister(input.uncheckedRegister()));
				continue;
			}

			if (requirement.limitedTo().size() != 1) {
				throw new InternalCompilerException("Input has multiple options: " + input + " - " + requirement);
			}
			X86Register wantedRegister = requirement.limitedTo().iterator().next();
			if (freeRegisters.remove(wantedRegister)) {
				input.register(wantedRegister);
				liveNodes.add(input);

				// Update created spillnode
				updateSpillNode(input, spillNodes.get(input).withRegister(input.uncheckedRegister()));
				continue;
			}

			displaceSpecific(input, instruction, wantedRegister);
		}
	}

	private void allocateOutputs(LegoNode instruction) {
		for (LegoNode result : instruction.results()) {
			// Only one user
			//  => The requirements of that user should be respected
			//  => let them handle it in the input code
			if (uses.uses(result).size() == 1) {
				//				continue;
			}

			// If we already have a spill node for this result it either has a register or should be on the stack
			if (spillNodes.containsKey(result)) {
				continue;
			}
			updateSpillNode(result, SpillNode.forNode(result, spillContext));

			if (freeRegisters.isEmpty()) {
				continue;
			}

			// If it is the last use of the value don't bother allocating a register
			if (uses.isLastUse(liveliness, result, result)) {
				continue;
			}

			if (!result.registerRequirement().limited()) {
				result.register(allocateNextRegister());
				liveNodes.add(result);

				// Update created spillnode
				updateSpillNode(result, spillNodes.get(result).withRegister(result.uncheckedRegister()));
				continue;
			}

			if (result.registerRequirement().limitedTo().size() != 1) {
				throw new InternalCompilerException("Output has multiple options: " + result);
			}

			X86Register wantedRegister = result.registerRequirement().limitedTo().iterator().next();

			if (freeRegisters.remove(wantedRegister)) {
				result.register(wantedRegister);
				liveNodes.add(result);

				// Update created spillnode
				updateSpillNode(result, spillNodes.get(result).withRegister(result.uncheckedRegister()));
				continue;
			}

			displaceSpecific(result, instruction, wantedRegister);
		}
	}

	private void updateSpillNode(LegoNode result, SpillNode spillNode) {
		spillNodes.put(result, spillNode);
	}

	private void displaceSpecific(LegoNode newNode, LegoNode instruction, X86Register wantedRegister) {
		LegoNode conflicting = liveNodes.stream()
			.filter(it -> it.uncheckedRegister() == wantedRegister)
			.findFirst()
			.orElseThrow(() -> new InternalCompilerException("Thought I was constrained but wasn't"));

		// Conflicting was dead, throw it away and replace it
		if (!liveliness.getLiveBefore(instruction, dominance).contains(conflicting)) {
			liveNodes.remove(conflicting);
			newNode.register(wantedRegister);

			// Update created spillnode
			updateSpillNode(newNode, spillNodes.get(newNode).withRegister(newNode.uncheckedRegister()));
		}

		// Conflicting is still alive, so newNode sadly will not receive a physical register. It will be reloaded and
		// conflicting spilled around the instruction instead
	}

	private void reloadAndSpillArgumentsAndResults(LegoPlate block) {
		for (LegoNode legoNode : block.nodes()) {
			SpillNode node = spillNodes.get(legoNode);

			for (int i = 0; i < node.lego().inputs().size(); i++) {
				LegoNode input = node.lego().inputs().get(i);
				if (input.registerIgnore()) {
					continue;
				}
				if (!spillNodes.get(input).hasRegister()) {
					node.reloadArgument(input, i);
				} else {
					X86Register inputRegister = spillNodes.get(input).getRegister();
					LegoRegisterRequirement requirement = node.lego().inRequirements().get(i);
					if (requirement.limited() && !requirement.limitedTo().contains(inputRegister)) {
						node.moveArgumentTo(input, i, requirement.limitedTo().iterator().next());
					}
				}
			}

			if (!node.lego().registerIgnore()) {
				// Spill results which were not assigned a register
				for (LegoNode result : node.lego().results()) {
					if (result.registerIgnore()) {
						continue;
					}
					if (spillNodes.get(result).hasRegister()) {
						continue;
					}

					node.spillResult(result);
				}
			}
		}
	}

	/**
	 * Fix
	 * <ul>
	 *     <li>Clobbers that conflict with live nodes</li>
	 *     <li>Outputs that conflict with live nodes</li>
	 * </ul>
	 *
	 * @param block the block to fix them in
	 */
	private void fixClobbers(LegoPlate block) {
		for (LegoNode node : block.nodes()) {
			// Skip our inserted nodes
			if (node instanceof LegoSpill || node instanceof LegoReload || node instanceof LegoMovRegister) {
				continue;
			}
			Set<LegoNode> live = liveliness.getLiveBefore(node, dominance);

			// Handle live nodes interfering with the results
			if (!node.registerIgnore()) {
				for (LegoNode result : node.results()) {
					// This node was assigned a register so it did not conflict
					if (spillNodes.get(result).hasRegister()) {
						continue;
					}
					// We need to spill the live node around this
					for (LegoNode liveNode : live) {
						if (liveNode.registerIgnore()) {
							continue;
						}
						if (uses.isLastUse(liveliness, liveNode, node)) {
							continue;
						}
						if (liveNode.uncheckedRegister().equals(result.uncheckedRegister())) {
							spillNodes.get(node).handleConflictingNode(liveNode);
						}
					}
				}
			}

			// Handle live nodes interfering with the clobbers
			if (!node.clobbered().isEmpty()) {
				for (LegoNode liveNode : live) {
					if (liveNode.registerIgnore()) {
						continue;
					}
					if (uses.isLastUse(liveliness, liveNode, node)) {
						continue;
					}
					if (node.clobbered().contains(liveNode.uncheckedRegister())) {
						spillNodes.get(node).handleConflictingNode(liveNode);
					}
				}
			}
		}
	}

	private X86Register allocateNextRegister() {
		X86Register register = freeRegisters.iterator().next();
		freeRegisters.remove(register);
		return register;
	}

	private void assignSpillSlots() {
		Map<LegoNode, Integer> slotIndices = new HashMap<>();
		for (LegoPlate block : controlFlowGraph.getAllBlocks()) {
			for (LegoNode node : block.nodes()) {
				if (node instanceof LegoReload reload) {
					int index = slotIndices.computeIfAbsent(reload.originalValue(), ignored -> slotIndices.size());
					reload.spillSlot(index);
				} else if (node instanceof LegoSpill spill) {
					int index = slotIndices.computeIfAbsent(spill.originalValue(), ignored -> slotIndices.size());
					spill.spillSlot(index);
				}
			}
		}
	}

	private record SpillNode(
		LegoNode lego,
		List<LegoSpill> spillsBefore,
		List<LegoSpill> spillsAfter,
		List<Pair<LegoReload, Integer>> reloadsBefore,
		List<LegoReload> reloadsAfter,
		List<Pair<Pair<LegoNode, LegoMovRegister>, Integer>> movesBefore,
		Optional<X86Register> register,
		SpillContext spillContext
	) {

		public static SpillNode forNode(LegoNode input, SpillContext spillContext) {
			return new SpillNode(input, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
				new ArrayList<>(), Optional.empty(), spillContext);
		}

		public boolean hasRegister() {
			return register.isPresent();
		}

		public X86Register getRegister() {
			return register.orElseThrow();
		}

		public SpillNode withRegister(X86Register register) {
			return new SpillNode(lego, spillsBefore, spillsAfter, reloadsBefore, reloadsAfter, movesBefore,
				Optional.of(register), spillContext);
		}

		/**
		 * Adds a result that must be spilled after the instruction is complete.
		 *
		 * @param result the result node to spill
		 */
		public void spillResult(LegoNode result) {
			LegoGraph graph = result.graph();
			LegoSpill spill = new LegoSpill(graph.nextId(), lego.block(), graph, result.size(), List.of(), result);

			if (spillContext.isDominated(spill, lego)) {
				return;
			}

			LOGGER.debug("Spilling result %s after %s", result, lego);
			spillsAfter.add(spill);
			spillContext.addSpill(spill, lego);

			LegoRegisterRequirement requirement = result.registerRequirement();
			if (requirement.limited()) {
				result.register(requirement.limitedTo().iterator().next());
			} else {
				// Randomly picked
				result.register(X86Register.R8);
			}
		}

		/**
		 * Reloads an argument so it can be passed to the node.
		 *
		 * @param argument the argument to reload
		 * @param index the index of the argument
		 */
		public void reloadArgument(LegoNode argument, int index) {
			LOGGER.debug("Reloading argument %s (%s) before %s", argument, index, lego);
			LegoGraph graph = argument.graph();
			var reload = new LegoReload(graph.nextId(), lego.block(), graph, argument.size(), List.of(), argument);
			reloadsBefore.add(new Pair<>(reload, index));

			LegoRegisterRequirement requirement = lego.inRequirements().get(index);
			if (requirement.limited()) {
				reload.register(requirement.limitedTo().iterator().next());
			} else {
				// Must be distinct to all other args
				reload.register(X86Register.values()[index]);
			}
		}

		/**
		 * Moves an argument to a different register to fulfill some constraints.
		 *
		 * @param argument the argument to move
		 * @param index the index of the argument
		 * @param wantedRegister the register to move it to
		 */
		public void moveArgumentTo(LegoNode argument, int index, X86Register wantedRegister) {
			LOGGER.debug("Moving argument %s (%s) before %s to %s", argument, index, lego, wantedRegister);
			LegoGraph graph = argument.graph();
			var move = new LegoMovRegister(graph.nextId(), lego.block(), graph, argument.size(), List.of());
			move.register(wantedRegister);
			movesBefore.add(new Pair<>(new Pair<>(argument, move), index));

			// Spill around to be sure it is not destroyed
			//			handleConflictingNode(argument);
		}

		/**
		 * Registers a conflicting node that must be spilled before and reloaded after this instruction is complete.
		 *
		 * @param conflicting the node that conflicts
		 */
		public void handleConflictingNode(LegoNode conflicting) {
			LegoGraph graph = conflicting.graph();
			LegoSpill spill =
				new LegoSpill(graph.nextId(), lego.block(), graph, conflicting.size(), List.of(), conflicting);

			if (spillContext.isDominated(spill, lego)) {
				return;
			}
			LOGGER.debug("Spilling %s around %s", conflicting, lego);
			spillsBefore.add(spill);
			spillContext.addSpill(spill, lego);

			LegoReload reload =
				new LegoReload(graph.nextId(), lego.block(), graph, conflicting.size(), List.of(), conflicting);
			reloadsAfter.add(reload);
			reload.register(conflicting.uncheckedRegister());
		}

		public void clear() {
			spillsBefore.clear();
			spillsAfter.clear();

			reloadsBefore.clear();
			reloadsAfter.clear();

			movesBefore.clear();
		}
	}

	private record RewireCleanup(
		LegoNode source,
		int index,
		LegoNode newArgument
	) {

	}

	private record SpillContext(
		Map<LegoNode, Set<LegoNode>> spillsForNode,
		Dominance dominance
	) {
		public boolean isDominated(LegoSpill newSpill, LegoNode spillBefore) {
			for (LegoNode existingBefore : spillsForNode.getOrDefault(newSpill.originalValue(), Set.of())) {
				if (dominance.dominates(existingBefore, spillBefore) || existingBefore.equals(spillBefore)) {
					return true;
				}
			}
			return false;
		}

		public void addSpill(LegoSpill spill, LegoNode before) {
			spillsForNode.computeIfAbsent(spill.originalValue(), ignored -> new HashSet<>()).add(before);
		}
	}

}
