package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.Relation;
import firm.nodes.Cond;

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
}
