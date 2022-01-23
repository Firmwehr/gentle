package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;
import firm.nodes.Shrs;

import java.util.List;

public class IkeaShrs implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Shrs shr;
	private final IkeaBløck block;

	public IkeaShrs(IkeaBøx box, IkeaNode left, IkeaNode right, Shrs shr, IkeaBløck block) {
		this.box = box;
		this.left = left;
		this.right = right;
		this.shr = shr;
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
	public IkeaBløck getBlock() {
		return block;
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(shr);
	}
}
