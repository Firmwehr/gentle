package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Mul;
import firm.nodes.Node;

import java.util.List;

public class IkeaMul implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Mul mul;
	private final IkeaBløck block;

	public IkeaMul(IkeaBøx box, IkeaNode left, IkeaNode right, Mul mul, IkeaBløck block) {
		this.box = box;
		this.left = left;
		this.right = right;
		this.mul = mul;
		this.block = block;
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

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(mul);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}
}
