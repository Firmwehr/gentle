package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

// TODO: replace with dummy, needs to be integrated in IkeaNode
public class IkeaMovStoreEx extends IkeaNode {

	private final BoxScheme scheme;

	public IkeaMovStoreEx(
		int id, IkeaBløck block, IkeaGraph graph, List<Node> firmNodes, BoxScheme scheme
	) {
		super(id, block, graph, IkeaRegisterSize.ILLEGAL, firmNodes);

		this.scheme = scheme;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public BoxScheme scheme() {
		return scheme;
	}

	// TODO: FInd useful values
	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister(), IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.none();
	}

}
