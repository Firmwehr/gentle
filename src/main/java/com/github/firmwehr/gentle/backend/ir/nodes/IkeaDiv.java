package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaDiv implements IkeaNode {
	private IkeaBøx boxQuotient;
	private IkeaBøx boxMod;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Node node;
	private final Result result;
	private final IkeaBløck block;

	public IkeaDiv(
		IkeaBøx boxQuotient, IkeaBøx boxMod, IkeaNode left, IkeaNode right, Node node, Result result, IkeaBløck block
	) {
		this.boxQuotient = boxQuotient;
		this.boxMod = boxMod;
		this.left = left;
		this.right = right;
		this.node = node;
		this.result = result;
		this.block = block;
	}

	@Override
	public IkeaBøx box() {
		return result == Result.MOD ? boxMod : boxQuotient;
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

	public Node getNode() {
		return node;
	}

	public Result getResult() {
		return result;
	}

	public IkeaBøx getBoxQuotient() {
		return boxQuotient;
	}

	public IkeaBøx getBoxMod() {
		return boxMod;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(node);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return null;
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		return null;
	}

	public enum Result {
		QUOTIENT,
		MOD
	}
}
