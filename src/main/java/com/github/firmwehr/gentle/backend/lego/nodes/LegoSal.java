package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

public class LegoSal extends LegoShift {

	public LegoSal(
		int id,
		LegoPlate block,
		LegoGraph graph,
		LegoBøx.LegoRegisterSize size,
		List<Node> firmNodes,
		LegoConst shiftValue
	) {
		super(id, block, graph, size, firmNodes, shiftValue);
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
