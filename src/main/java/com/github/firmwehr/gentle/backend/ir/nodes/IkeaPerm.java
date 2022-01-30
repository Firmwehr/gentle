package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaPerm extends IkeaNode {

	public IkeaPerm(
		int id, IkeaBløck block, IkeaGraph graph, List<Node> firmNodes
	) {
		super(id, block, graph, IkeaRegisterSize.ILLEGAL, firmNodes);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return graph().getInputs(this).stream().map(IkeaNode::registerRequirement).toList();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.none();
	}
}
