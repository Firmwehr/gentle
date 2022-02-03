package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.TargetValue;
import firm.nodes.Node;

import java.util.List;

public final class LegoConst extends LegoNode {

	private final TargetValue value;

	public LegoConst(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes,
		TargetValue value
	) {
		super(id, block, graph, size, firmNodes);
		this.value = value;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of();
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.gpRegister();
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " " + value.asLong() + " (" + id() + ")";
	}

	public TargetValue value() {
		return value;
	}
}
