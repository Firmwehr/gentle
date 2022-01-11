package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Cmp;
import firm.nodes.Node;

import java.util.List;

public class IkeaCmp implements IkeaNode {
	private final IkeaNode left;
	private final IkeaNode right;
	private final Cmp cmp;
	private final boolean wasInverted;

	public IkeaCmp(IkeaNode left, IkeaNode right, Cmp cmp, boolean wasInverted) {
		this.left = left;
		this.right = right;
		this.cmp = cmp;
		this.wasInverted = wasInverted;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx(IkeaRegisterSize.ILLEGAL);
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

	public boolean wasInverted() {
		return wasInverted;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(cmp);
	}

}
