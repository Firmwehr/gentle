package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.Relation;
import firm.nodes.Node;

import java.util.List;

public class IkeaJcc extends IkeaNode {

	private final Relation relation;
	private final IkeaBløck trueTarget;
	private final IkeaBløck falseTarget;

	public IkeaJcc(
		int id,
		IkeaBløck block,
		IkeaGraph graph,
		List<Node> firmNodes,
		Relation relation,
		IkeaBløck trueTarget,
		IkeaBløck falseTarget
	) {
		super(id, block, graph, IkeaRegisterSize.ILLEGAL, firmNodes);
		this.relation = relation;
		this.trueTarget = trueTarget;
		this.falseTarget = falseTarget;
	}

	public Relation relation() {
		return relation;
	}

	public IkeaBløck trueTarget() {
		return trueTarget;
	}

	public IkeaBløck falseTarget() {
		return falseTarget;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.none();
	}
}
