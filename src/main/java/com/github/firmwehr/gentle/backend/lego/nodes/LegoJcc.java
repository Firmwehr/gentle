package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.Relation;
import firm.nodes.Node;

import java.util.List;

public class LegoJcc extends LegoNode {

	private final Relation relation;
	private final LegoPlate trueTarget;
	private final LegoPlate falseTarget;

	public LegoJcc(
		int id,
		LegoPlate block,
		LegoGraph graph,
		List<Node> firmNodes,
		Relation relation,
		LegoPlate trueTarget,
		LegoPlate falseTarget
	) {
		super(id, block, graph, LegoRegisterSize.ILLEGAL, firmNodes);
		this.relation = relation;
		this.trueTarget = trueTarget;
		this.falseTarget = falseTarget;
	}

	public Relation relation() {
		return relation;
	}

	public LegoPlate trueTarget() {
		return trueTarget;
	}

	public LegoPlate falseTarget() {
		return falseTarget;
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
}
