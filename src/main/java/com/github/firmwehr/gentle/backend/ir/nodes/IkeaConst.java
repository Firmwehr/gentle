package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.TargetValue;
import firm.nodes.Node;

import java.util.List;

public final class IkeaConst extends IkeaNode {

	private final TargetValue value;

	public IkeaConst(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes,
		TargetValue value
	) {
		super(id, block, graph, size, firmNodes);
		this.value = value;
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
		return IkeaRegisterRequirement.gpRegister();
	}

	@Override
	public String display() {
		return "IkeaConst " + value.asLong() + " (" + id() + ")";
	}

	public TargetValue value() {
		return value;
	}
}
