package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;

import static com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement.singleRegister;

public class IkeaRet extends IkeaNode {

	public IkeaRet(
		int id, IkeaBløck block, IkeaGraph graph, List<Node> firmNodes
	) {
		super(id, block, graph, IkeaBøx.IkeaRegisterSize.ILLEGAL, firmNodes);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		List<IkeaNode> inputs = graph().getInputs(this);
		List<IkeaRegisterRequirement> requirements = new ArrayList<>();

		return inputs.stream().map(node -> singleRegister(X86Register.RAX)).toList();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.none();
	}

}
