package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.nodes.Sub;

import java.util.List;

public class IkeaSub implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Sub sub;

	public IkeaSub(IkeaBøx box, IkeaNode left, IkeaNode right, Sub sub) {
		this.box = box;
		this.left = left;
		this.right = right;
		this.sub = sub;
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
