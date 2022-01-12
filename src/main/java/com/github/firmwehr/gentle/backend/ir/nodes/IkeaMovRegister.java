package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.Collections;
import java.util.List;

public class IkeaMovRegister implements IkeaNode {
	private final IkeaBøx source;
	private final IkeaBøx target;
	private final IkeaRegisterSize size;

	public IkeaMovRegister(IkeaBøx source, IkeaBøx target, IkeaRegisterSize size) {
		this.source = source;
		this.target = target;
		this.size = size;
	}

	@Override
	public IkeaBøx box() {
		return target;
	}

	@Override
	public List<IkeaNode> parents() {
		return Collections.emptyList();
	}

	public IkeaBøx getSource() {
		return source;
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
		return List.of();
	}

}
