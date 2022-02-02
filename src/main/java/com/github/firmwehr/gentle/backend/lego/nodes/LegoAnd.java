package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

public final class LegoAnd extends LegoBinaryOp {

	public LegoAnd(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

}
