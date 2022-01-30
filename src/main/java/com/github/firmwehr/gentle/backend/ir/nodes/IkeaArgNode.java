package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaArgNode extends IkeaNode {

	private final int index;

	public IkeaArgNode(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes, int index
	) {
		super(id, block, graph, size, firmNodes);
		this.index = index;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		// passed in a register
		if (isPassedInRegister()) {
			return IkeaRegisterRequirement.singleRegister(IkeaCall.REGISTER_ORDER.get(index));
		}
		// passed on the stack, just reserve whatever register for it
		return IkeaRegisterRequirement.gpRegister();
	}

	public boolean isPassedInRegister() {
		return index < IkeaCall.REGISTER_ORDER.size();
	}

	@Override
	public String display() {
		return "IkeaArg " + index + " (" + id() + ")";
	}
}
