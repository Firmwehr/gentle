package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import firm.nodes.Node;

import java.util.List;

// TODO: javadoc. this is the most common case, so we simply create a new base class for it
public abstract class LegoBinaryOp extends LegoNode {

	protected LegoBinaryOp(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	public LegoNode left() {
		return inputs().get(0);
	}

	public LegoNode right() {
		return inputs().get(1);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of(LegoRegisterRequirement.gpRegister(), LegoRegisterRequirement.gpRegister());
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.gpRegister();
	}
}
