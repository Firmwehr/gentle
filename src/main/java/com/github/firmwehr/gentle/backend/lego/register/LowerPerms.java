package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.codegen.RegisterTransferGraph;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoMovRegister;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPerm;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoProj;
import com.github.firmwehr.gentle.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LowerPerms {

	private final LegoGraph legoGraph;
	private final ControlFlowGraph controlFlowGraph;

	public LowerPerms(LegoGraph legoGraph, ControlFlowGraph controlFlowGraph) {
		this.legoGraph = legoGraph;
		this.controlFlowGraph = controlFlowGraph;
	}

	public void lower() {
		for (LegoPlate block : controlFlowGraph.getAllBlocks()) {
			block.nodes()
				.stream()
				.filter(it -> it instanceof LegoPerm)
				.map(it -> (LegoPerm) it)
				.toList() // make a copy as we modify block.nodes()
				.forEach(this::lowerPerm);
		}
	}

	private void lowerPerm(LegoPerm perm) {
		List<LegoProj> outputs = legoGraph.getOutputs(perm)
			.stream()
			.map(it -> (LegoProj) it)
			.sorted(Comparator.comparingInt(LegoProj::index))
			.toList();
		List<LegoNode> inputs = legoGraph.getInputs(perm);

		if (outputs.size() != inputs.size()) {
			throw new InternalCompilerException("Perm output counts do not match. In: " + inputs + ", Out: " + outputs);
		}

		// Compute all registers not used as in or outputs. They can be used to break cycles.
		Set<X86Register> freeRegisters = X86Register.all();
		for (LegoProj output : outputs) {
			freeRegisters.remove(output.uncheckedRegister());
		}
		for (LegoNode input : inputs) {
			freeRegisters.remove(input.uncheckedRegister());
		}

		RegisterTransferGraph<X86Register> transferGraph = new RegisterTransferGraph<>(freeRegisters);

		for (int i = 0; i < inputs.size(); i++) {
			transferGraph.addMove(inputs.get(i).uncheckedRegister(), outputs.get(i).uncheckedRegister());
		}

		// Keep track of which node currently represents which register. This is not needed for our normal inputs, but
		// a temporary register might be used in multiple places and we need to keep track of the last move to add
		// another to
		Map<X86Register, LegoNode> registerMap =
			inputs.stream().collect(Collectors.toMap(LegoNode::uncheckedRegister, it -> it));

		// Insert register copies
		int insertionIndex = perm.block().nodes().indexOf(perm);
		for (Pair<X86Register, X86Register> pair : transferGraph.generateMoveSequence()) {
			LegoNode inNode = registerMap.get(pair.first());

			LegoMovRegister move =
				new LegoMovRegister(legoGraph.nextId(), perm.block(), legoGraph, inNode.size(), List.of());

			move.register(pair.second());

			perm.block().nodes().add(insertionIndex++, move);
			legoGraph.addNode(move, List.of(inNode));
			registerMap.put(pair.second(), move);
		}

		// Rewire users of output projs to correct move (last in that register)
		for (LegoProj output : outputs) {
			for (LegoGraph.LegoEdge useEdge : Set.copyOf(legoGraph.getOutputEdges(output))) {
				legoGraph.setInput(useEdge.src(), useEdge.index(), registerMap.get(output.uncheckedRegister()));
			}
		}

		// Clean up and delete the now unreachable perm and Projs
		legoGraph.removeNode(perm);
		outputs.forEach(legoGraph::removeNode);
	}
}
