package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import firm.Relation;
import firm.nodes.Cond;

import java.util.List;

public class IkeaJcc implements IkeaNode {
	private final IkeaBløck trueTarget;
	private final IkeaBløck falseTarget;
	private final Cond cond;
	private final Relation relation;
	private final IkeaNode parent;


	public IkeaJcc(IkeaBløck trueTarget, IkeaBløck falseTarget, Cond cond, Relation relation, IkeaNode parent) {
		this.trueTarget = trueTarget;
		this.falseTarget = falseTarget;
		this.cond = cond;
		this.relation = relation;
		this.parent = parent;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx();
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(this.parent);
	}
}
