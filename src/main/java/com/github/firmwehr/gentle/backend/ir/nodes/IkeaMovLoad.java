package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;

import java.util.List;

public class IkeaMovLoad implements IkeaNode {
	private final IkeaBøx register;
	private final List<IkeaNode> parents;

	public IkeaMovLoad(IkeaBøx register, List<IkeaNode> parents) {
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
}
