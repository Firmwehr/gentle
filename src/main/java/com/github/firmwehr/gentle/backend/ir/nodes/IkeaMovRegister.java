package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;

import java.util.List;

public class IkeaMovRegister implements IkeaNode {
	private final IkeaBøx register;
	private final List<IkeaNode> parents;

	public IkeaMovRegister(IkeaBøx register, List<IkeaNode> parents) {
		this.register = register;
		this.parents = parents;
	}

	@Override
	public IkeaBøx box() {
		return register;
	}

	@Override
	public List<IkeaNode> parents() {
		return parents;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
