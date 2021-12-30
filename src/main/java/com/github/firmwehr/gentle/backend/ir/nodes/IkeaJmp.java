package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;

import java.util.List;

public class IkeaJmp implements IkeaNode {
	private final IkeaBløck target;

	public IkeaJmp(IkeaBløck target) {
		this.target = target;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx();
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}
}
