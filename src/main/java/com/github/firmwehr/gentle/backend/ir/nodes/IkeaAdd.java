package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Add;

import java.util.List;

public class IkeaAdd implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Add add;

	public IkeaAdd(IkeaBøx box, IkeaNode left, IkeaNode right, Add add) {
		this.box = box;
		this.left = left;
		this.right = right;
		this.add = add;
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
