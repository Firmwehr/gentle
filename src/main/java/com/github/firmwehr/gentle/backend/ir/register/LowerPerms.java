package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.codegen.RegisterTransferGraph;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaMovRegister;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaPerm;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaProj;
import com.github.firmwehr.gentle.util.Mut;
import com.github.firmwehr.gentle.util.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class LowerPerms {

	private final IkeaGraph ikeaGraph;
	private final ControlFlowGraph controlFlowGraph;

	public LowerPerms(IkeaGraph ikeaGraph, ControlFlowGraph controlFlowGraph) {
		this.ikeaGraph = ikeaGraph;
		this.controlFlowGraph = controlFlowGraph;
	}

	public void lower() {
		for (IkeaBløck block : controlFlowGraph.getAllBlocks()) {
			block.nodes()
				.stream()
				.filter(it -> it instanceof IkeaPerm)
				.map(it -> (IkeaPerm) it)
				.toList() // make a copy as we modify block.nodes()
				.forEach(this::lowerPerm);
		}
	}

	private void lowerPerm(IkeaPerm perm) {
		List<IkeaProj> outputs = ikeaGraph.getOutputs(perm)
			.stream()
			.map(it -> (IkeaProj) it)
			.sorted(Comparator.comparingInt(IkeaProj::index))
			.toList();
		List<IkeaNode> inputs = ikeaGraph.getInputs(perm);

		if (outputs.size() != inputs.size()) {
			throw new InternalCompilerException("Perm output counts do not match. In: " + inputs + ", Out: " + outputs);
		}

		// Compute all registers not used as in or outputs. They can be used to break cycles.
		Set<X86Register> freeRegisters = X86Register.all();
		for (IkeaProj output : outputs) {
			freeRegisters.remove(output.uncheckedRegister());
		}
		for (IkeaNode input : inputs) {
			freeRegisters.remove(input.uncheckedRegister());
		}

		RegisterTransferGraph<X86Register> transferGraph = new RegisterTransferGraph<>(freeRegisters);

		for (int i = 0; i < inputs.size(); i++) {
			transferGraph.addMove(inputs.get(i).uncheckedRegister(), outputs.get(i).uncheckedRegister());
		}

		// Keep track of which node currently represents which register. This is not needed for our normal inputs, but
		// a temporary register might be used in multiple places and we need to keep track of the last move to add
		// another to
		Map<X86Register, IkeaNode> registerMap =
			inputs.stream().collect(Collectors.toMap(IkeaNode::uncheckedRegister, it -> it));

		// Insert register copies
		int insertionIndex = perm.block().nodes().indexOf(perm);
		for (Pair<X86Register, X86Register> pair : transferGraph.generateMoveSequence()) {
			IkeaMovRegister move =
				new IkeaMovRegister(new Mut<>(Optional.of(pair.second())), perm.block(), ikeaGraph, List.of(),
					ikeaGraph.nextId());

			perm.block().nodes().add(insertionIndex++, move);
			ikeaGraph.addNode(move, List.of(registerMap.get(pair.first())));
			registerMap.put(pair.second(), move);
		}

		// Rewire users of output projs to correct move (last in that register)
		for (IkeaProj output : outputs) {
			for (IkeaGraph.IkeaEdge useEdge : Set.copyOf(ikeaGraph.getOutputEdges(output))) {
				ikeaGraph.setInput(useEdge.src(), useEdge.index(), registerMap.get(output.uncheckedRegister()));
			}
		}

		// Clean up and delete the now unreachable perm and Projs
		ikeaGraph.removeNode(perm);
		outputs.forEach(ikeaGraph::removeNode);
	}
}
