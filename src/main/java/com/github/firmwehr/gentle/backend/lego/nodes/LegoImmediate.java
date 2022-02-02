package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.TargetValue;
import firm.nodes.Const;

import java.util.List;

public class LegoImmediate extends LegoNode {
	private final TargetValue targetValue;

	public LegoImmediate(
		int id,
		LegoPlate block,
		LegoGraph graph,
		Const firmNode
	) {
		super(id, block, graph, LegoBøx.LegoRegisterSize.ILLEGAL, List.of(firmNode));
		this.targetValue = firmNode.getTarval();
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

	public TargetValue targetValue() {
		return targetValue;
	}
}
