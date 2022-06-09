package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.Relation;
import firm.nodes.Node;

import java.util.List;

public class LegoSetcc extends LegoNode {

	private final Relation relation;

	public LegoSetcc(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes,
		Relation relation
	) {
		super(id, block, graph, size, firmNodes);
		this.relation = relation;
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

	public Relation relation() {
		return relation;
	}
}
