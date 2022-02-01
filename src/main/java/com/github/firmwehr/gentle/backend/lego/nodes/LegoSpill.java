package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

public class LegoSpill extends LegoNode {

	private int spillSlot;
	private final LegoNode originalValue;

	public LegoSpill(
		int id,
		LegoPlate block,
		LegoGraph graph,
		LegoBøx.LegoRegisterSize size,
		List<Node> firmNodes,
		LegoNode originalValue
	) {
		super(id, block, graph, size, firmNodes);
		this.originalValue = originalValue;
	}

	public int spillSlot() {
		return spillSlot;
	}

	public void spillSlot(int spillSlot) {
		this.spillSlot = spillSlot;
	}

	public LegoNode originalValue() {
		return originalValue;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of(LegoRegisterRequirement.gpRegister());
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.none();
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " " + spillSlot + " (" + id() + ")";
	}
}
