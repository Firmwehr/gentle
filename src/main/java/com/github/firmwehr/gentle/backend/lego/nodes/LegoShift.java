package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import firm.nodes.Node;

import java.util.List;

public abstract class LegoShift extends LegoNode {

	private final LegoConst shiftValue;

	public LegoShift(
		int id,
		LegoPlate block,
		LegoGraph graph,
		LegoBøx.LegoRegisterSize size,
		List<Node> firmNodes,
		LegoConst shiftValue
	) {
		super(id, block, graph, size, firmNodes);
		this.shiftValue = shiftValue;
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of(LegoRegisterRequirement.gpRegister());
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.gpRegister();
	}

	public LegoConst shiftValue() {
		return shiftValue;
	}
}
