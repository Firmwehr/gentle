package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Load;
import firm.nodes.Node;

import java.util.List;

public class IkeaMovLoad implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode address;
	private final IkeaRegisterSize size;
	private final Load node;

	public IkeaMovLoad(IkeaBøx box, IkeaNode address, IkeaRegisterSize size, Load node) {
		this.box = box;
		this.address = address;
		this.size = size;
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

	public IkeaNode getAddress() {
		return address;
	}

	public IkeaRegisterSize getSize() {
		return size;
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
