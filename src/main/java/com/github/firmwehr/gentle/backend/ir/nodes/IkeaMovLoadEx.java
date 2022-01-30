package com.github.firmwehr.gentle.backend.ir.nodes;


import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

// TODO: replace with dummy, needs to be integrated in IkeaNode
public class IkeaMovLoadEx extends IkeaNode {

	private final BoxScheme scheme;

	/**
	 * @param id The id of the node.
	 * @param block The parent block of the node.
	 * @param graph The associated graph.
	 * @param size The register size of the resulting value.
	 * @param firmNodes A list of firm nodes that are part of this ikea node.
	 * @param scheme the addressing scheme
	 */
	public IkeaMovLoadEx(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes, BoxScheme scheme
	) {
		super(id, block, graph, size, firmNodes);
		this.scheme = scheme;
	}

	public BoxScheme scheme() {
		return scheme;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	// TODO: Figure sth out here
	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.gpRegister();
	}

}
