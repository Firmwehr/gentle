package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx.LegoRegisterSize;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

// TODO: replace with dummy, needs to be integrated in LegoNode
public class LegoMovStoreEx extends LegoNode {

	private final BoxScheme scheme;

	public LegoMovStoreEx(
		int id, LegoPlate block, LegoGraph graph, List<Node> firmNodes, BoxScheme scheme
	) {
		super(id, block, graph, LegoRegisterSize.ILLEGAL, firmNodes);

		this.scheme = scheme;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public BoxScheme scheme() {
		return scheme;
	}

	// TODO: FInd useful values
	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of(LegoRegisterRequirement.gpRegister(), LegoRegisterRequirement.gpRegister());
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.none();
	}

}
