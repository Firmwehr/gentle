package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Conv;
import firm.nodes.Node;

import java.util.List;

public class IkeaConv implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode parent;
	private final IkeaRegisterSize sourceSize;
	private final IkeaRegisterSize targetSize;
	private final Conv conv;

	public IkeaConv(
		IkeaBøx box, IkeaNode parent, IkeaRegisterSize sourceSize, IkeaRegisterSize targetSize, Conv conv
	) {
		this.box = box;
		this.parent = parent;
		this.sourceSize = sourceSize;
		this.targetSize = targetSize;
		this.conv = conv;
	}

	@Override
	public IkeaBøx box() {
		return this.box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(parent);
	}

	public IkeaNode getParent() {
		return parent;
	}

	public IkeaRegisterSize getSourceSize() {
		return sourceSize;
	}

	public IkeaRegisterSize getTargetSize() {
		return targetSize;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(conv);
	}

}
