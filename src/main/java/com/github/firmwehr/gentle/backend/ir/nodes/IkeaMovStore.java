package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Store;

import java.util.List;

public class IkeaMovStore implements IkeaNode {
	private final IkeaNode value;
	private final IkeaNode address;
	private final IkeaRegisterSize size;
	private final Store node;

	public IkeaMovStore(IkeaNode value, IkeaNode address, IkeaRegisterSize size, Store node) {
		this.value = value;
		this.address = address;
		this.size = size;
		this.node = node;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx(IkeaRegisterSize.ILLEGAL);
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(address);
	}

	public IkeaRegisterSize getSize() {
		return size;
	}

	public IkeaNode getValue() {
		return value;
	}

	public IkeaNode getAddress() {
		return address;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
