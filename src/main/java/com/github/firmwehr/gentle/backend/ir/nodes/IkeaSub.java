package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Sub;

import java.util.List;
import java.util.Objects;

public class IkeaSub implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Sub sub;

	public IkeaSub(IkeaBøx box, IkeaNode left, IkeaNode right, Sub sub) {
		this.box = box;
		this.left = Objects.requireNonNull(left, "left can not be null!");
		this.right = Objects.requireNonNull(right, "right can not be null!");
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

	public IkeaNode getLeft() {
		return left;
	}

	public IkeaNode getRight() {
		return right;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
