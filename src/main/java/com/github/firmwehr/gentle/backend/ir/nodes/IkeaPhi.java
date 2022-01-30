package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaPhi extends IkeaNode {

	/**
	 * @param id The id of the node.
	 * @param block The parent block of the node.
	 * @param graph The associated graph.
	 * @param size The register size of the resulting value.
	 * @param firmNodes A list of firm nodes that are part of this ikea node.
	 */
	public IkeaPhi(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public IkeaNode parent(IkeaBløck parentBlock) {
		List<IkeaParentBløck> parents = block().parents();
		for (int i = 0; i < parents.size(); i++) {
			if (parents.get(i).parent().equals(parentBlock)) {
				return graph().getInputs(this).get(i);
			}
		}
		throw new InternalCompilerException("Could not find parent block " + parentBlock);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return graph().getInputs(this).stream().map(IkeaNode::registerRequirement).toList();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.gpRegister();
	}

}
