package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

public final class LegoProj extends LegoNode {

	private final int index;
	private final String nameForIndex;

	private LegoRegisterRequirement registerRequirement;

	public LegoProj(
		int id,
		LegoPlate block,
		LegoGraph graph,
		LegoBøx.LegoRegisterSize size,
		List<Node> firmNodes,
		int index,
		String nameForIndex
	) {
		super(id, block, graph, size, firmNodes);
		this.index = index;
		this.nameForIndex = nameForIndex;
		this.registerRequirement = LegoRegisterRequirement.gpRegister();
	}

	public String nameForIndex() {
		return nameForIndex;
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
		return registerRequirement;
	}

	public void registerRequirement(LegoRegisterRequirement regRequirement) {
		this.registerRequirement = regRequirement;
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " " + index + ": " + nameForIndex() + " (" + id() + ")";
	}

	public int index() {
		return index;
	}
}
