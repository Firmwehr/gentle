package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.Relation;
import firm.nodes.Cond;
import firm.nodes.Node;

import java.util.List;

public class IkeaSet implements IkeaNode {
	private IkeaBøx box;
	private final Cond cond;
	private final Relation relation;
	private final IkeaNode parent;

	public IkeaSet(IkeaBøx box, Cond cond, Relation relation, IkeaNode parent) {
		this.box = box;
		this.cond = cond;
		this.relation = relation;
		this.parent = parent;
	}

	@Override
	public IkeaBøx box() {
		return this.box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(this.parent);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(cond);
	}
}
