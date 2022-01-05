package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.nodes.Proj;

import java.util.List;

public class IkeaArgNode implements IkeaNode {
	private IkeaBøx box;
	private final Proj proj;

	public IkeaArgNode(IkeaBøx box, Proj proj) {
		this.box = box;
		this.proj = proj;
	}


	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}
}
