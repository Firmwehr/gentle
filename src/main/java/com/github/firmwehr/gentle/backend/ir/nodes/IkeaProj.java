package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public final class IkeaProj extends IkeaNode {

	private final int index;

	private IkeaRegisterRequirement regRequirement;

	public IkeaProj(
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
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return regRequirement;
	}

	public IkeaRegisterRequirement getRegRequirement() {
		return regRequirement;
	}

	public void setRegRequirement(IkeaRegisterRequirement regRequirement) {
		this.regRequirement = regRequirement;
	}

	@Override
	public String display() {
		return "IkeaProj " + index;
	}

	public int index() {
		return index;
	}
}
