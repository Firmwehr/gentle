package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

public final class LegoJmp extends LegoNode {

	private final LegoPlate target;

	public LegoJmp(
		int id, LegoPlate block, LegoGraph graph, List<Node> firmNodes, LegoPlate target
	) {
		super(id, block, graph, LegoRegisterSize.ILLEGAL, firmNodes);
		this.target = target;
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
		return LegoRegisterRequirement.none();
	}

	public LegoPlate target() {
		return target;
	}

}
