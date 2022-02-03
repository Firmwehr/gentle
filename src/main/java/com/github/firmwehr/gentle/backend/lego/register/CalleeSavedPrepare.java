package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoProj;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoRet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CalleeSavedPrepare {

	public static final Set<X86Register> CALLEE_SAVED =
		Set.of(X86Register.RBX, X86Register.RSP, X86Register.RBP, X86Register.R12, X86Register.R13, X86Register.R14,
			X86Register.R15);

	private final LegoGraph graph;
	private final ControlFlowGraph controlFlowGraph;

	public CalleeSavedPrepare(LegoGraph graph, ControlFlowGraph controlFlowGraph) {
		this.graph = graph;
		this.controlFlowGraph = controlFlowGraph;
	}

	public void prepare() {
		LegoPlate start = controlFlowGraph.getStart();
		List<LegoNode> projs = new ArrayList<>();

		for (X86Register register : CALLEE_SAVED) {
			LegoProj proj = new LegoProj(graph.nextId(), start, graph, LegoBøx.LegoRegisterSize.BITS_64, List.of(), 0,
				register.name());
			proj.register(register);
			proj.registerRequirement(LegoRegisterRequirement.singleRegister(register));

			start.nodes().add(0, proj);
			graph.addNode(proj, List.of());
			projs.add(proj);
		}

		// Add projs as return arguments
		for (LegoPlate end : controlFlowGraph.getEnds()) {
			for (LegoNode node : end.nodes()) {
				if (node instanceof LegoRet ret) {
					List<LegoNode> newInputs = new ArrayList<>(ret.inputs());
					newInputs.addAll(projs);
					graph.overwriteRet(ret, newInputs);
				}
			}
		}
	}

}
