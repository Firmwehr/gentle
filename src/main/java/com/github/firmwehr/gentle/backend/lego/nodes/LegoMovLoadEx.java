package com.github.firmwehr.gentle.backend.lego.nodes;


import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

// TODO: replace with dummy, needs to be integrated in LegoNode
public class LegoMovLoadEx extends LegoNode {

	private final BoxScheme scheme;

	/**
	 * @param id The id of the node.
	 * @param block The parent block of the node.
	 * @param graph The associated graph.
	 * @param size The register size of the resulting value.
	 * @param firmNodes A list of firm nodes that are part of this lego node.
	 * @param scheme the addressing scheme
	 */
	public LegoMovLoadEx(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes, BoxScheme scheme
	) {
		super(id, block, graph, size, firmNodes);
		this.scheme = scheme;
	}

	public BoxScheme scheme() {
		return scheme;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	// TODO: Figure sth out here
	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of(LegoRegisterRequirement.gpRegister());
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.gpRegister();
	}

}
