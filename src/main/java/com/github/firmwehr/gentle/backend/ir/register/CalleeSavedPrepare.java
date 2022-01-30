package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaProj;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaRet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CalleeSavedPrepare {

	public static final Set<X86Register> CALLEE_SAVED =
		Set.of(X86Register.RBX, X86Register.RSP, X86Register.RBP, X86Register.R12, X86Register.R13, X86Register.R14,
			X86Register.R15);

	private final IkeaGraph graph;
	private final ControlFlowGraph controlFlowGraph;

	public CalleeSavedPrepare(IkeaGraph graph, ControlFlowGraph controlFlowGraph) {
		this.graph = graph;
		this.controlFlowGraph = controlFlowGraph;
	}

	public void prepare() {
		IkeaBløck start = controlFlowGraph.getStart();
		List<IkeaNode> projs = new ArrayList<>();

		for (X86Register register : CALLEE_SAVED) {
			IkeaProj proj = new IkeaProj(graph.nextId(), start, graph, IkeaBøx.IkeaRegisterSize.BITS_64, List.of(), 0,
				register.name());
			proj.register(register);
			proj.registerRequirement(IkeaRegisterRequirement.singleRegister(register));

			start.nodes().add(0, proj);
			graph.addNode(proj, List.of());
			projs.add(proj);
		}

		// Add projs as return arguments
		for (IkeaBløck end : controlFlowGraph.getEnds()) {
			for (IkeaNode node : end.nodes()) {
				if (node instanceof IkeaRet ret) {
					List<IkeaNode> newInputs = new ArrayList<>(ret.inputs());
					newInputs.addAll(projs);
					graph.overwriteRet(ret, newInputs);
				}
			}
		}
	}

}
