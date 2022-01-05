package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Minus;

import java.util.List;

public class IkeaNeg implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode parent;
	private final Minus minus;

	public IkeaNeg(IkeaBøx box, IkeaNode parent, Minus minus) {
		this.box = box;
		this.parent = parent;
		this.minus = minus;
	}

	@Override
	public IkeaBøx box() {
		return this.box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(this.parent);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
