package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.nodes.Store;

import java.util.List;

public class IkeaMovStore implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode address;
	private final Store node;

	public IkeaMovStore(IkeaBøx box, IkeaNode address, Store node) {
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
}
