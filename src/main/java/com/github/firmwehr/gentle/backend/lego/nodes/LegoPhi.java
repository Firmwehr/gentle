package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.List;

public class LegoPhi extends LegoNode {

	/**
	 * @param id The id of the node.
	 * @param block The parent block of the node.
	 * @param graph The associated graph.
	 * @param size The register size of the resulting value.
	 * @param firmNodes A list of firm nodes that are part of this lego node.
	 */
	public LegoPhi(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public LegoNode parent(LegoPlate parentBlock) {
		List<LegoParentBløck> parents = block().parents();
		for (int i = 0; i < parents.size(); i++) {
			if (parents.get(i).parent().equals(parentBlock)) {
				return graph().getInputs(this).get(i);
			}
		}
		throw new InternalCompilerException("Could not find parent block " + parentBlock);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return graph().getInputs(this).stream().map(LegoNode::registerRequirement).toList();
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.gpRegister();
	}

}
