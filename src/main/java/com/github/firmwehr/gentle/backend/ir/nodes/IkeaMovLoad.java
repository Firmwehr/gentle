package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Load;

import java.util.List;

public class IkeaMovLoad implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode address;
	private final Load node;

	public IkeaMovLoad(IkeaBøx box, IkeaNode address, Load node) {
		this.box = box;
		this.address = address;
		this.node = node;
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(address);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
