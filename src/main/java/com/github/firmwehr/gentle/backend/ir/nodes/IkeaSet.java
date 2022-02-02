package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.Relation;
import firm.nodes.Node;

import java.util.List;

public class IkeaSet implements IkeaNode {
	private IkeaBøx box;
	private final Node underlying;
	private final IkeaNode parent;
	private final Relation relation;

	public IkeaSet(IkeaBøx box, Node underlying, IkeaNode parent, Relation relation) {
		this.box = box;
		this.underlying = underlying;
		this.parent = parent;
		this.relation = relation;
	}

	@Override
	public IkeaBøx box() {
		return this.box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(this.parent);
	}

	public Relation getRelation() {
		return relation;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(underlying);
	}
}
