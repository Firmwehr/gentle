package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.Mode;
import firm.nodes.Conv;

import java.util.List;

public class IkeaConv implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode parent;
	private final Mode sourceSize;
	private final Mode targetSize;
	private final Conv conv;

	public IkeaConv(IkeaBøx box, IkeaNode parent, Mode sourceSize, Mode targetSize, Conv conv) {
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

	public Mode getSourceSize() {
		return sourceSize;
	}

	public Mode getTargetSize() {
		return targetSize;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
