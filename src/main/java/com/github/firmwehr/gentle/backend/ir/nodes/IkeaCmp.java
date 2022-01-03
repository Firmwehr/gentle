package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.Relation;
import firm.nodes.Cmp;

import java.util.List;

public class IkeaCmp implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Cmp cmp;

	public IkeaCmp(
		IkeaBøx box, IkeaNode left, IkeaNode right, Cmp cmp
	) {
		this.box = box;
		this.left = left;
		this.right = right;
		this.cmp = cmp;
	}

	@Override
	public IkeaBøx box() {
		return this.box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(this.left, this.right);
	}
}
