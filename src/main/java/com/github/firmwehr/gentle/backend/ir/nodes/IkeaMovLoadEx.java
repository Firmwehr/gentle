package com.github.firmwehr.gentle.backend.ir.nodes;


import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Load;
import firm.nodes.Node;

import java.util.List;

public class IkeaMovLoadEx implements IkeaNode {
	private final IkeaNode base;
	private final IkeaNode index;
	private final int scale;
	private final long displacement;

	private final Load node;
	private IkeaBøx box;

	public IkeaMovLoadEx(IkeaBøx box, IkeaNode base, IkeaNode index, int scale, long displacement, Load node) {
		this.box = box;
		this.base = base;
		this.index = index;
		this.scale = scale;
		this.displacement = displacement;
		this.node = node;
	}

	public IkeaNode getBase() {
		return base;
	}

	public IkeaNode getIndex() {
		return index;
	}

	public int getScale() {
		return scale;
	}

	public long getDisplacement() {
		return displacement;
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(base, index);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(node);
	}
}
