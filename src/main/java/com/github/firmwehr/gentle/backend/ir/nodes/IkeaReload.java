package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaReload implements IkeaNode {
	private IkeaBøx box;
	private final IkeaBløck bløck;
	private final IkeaNode originalDef;

	public IkeaReload(IkeaBøx box, IkeaBløck bløck, IkeaNode originalDef) {
		this.box = box;
		this.bløck = bløck;
		this.originalDef = originalDef;
	}

	public IkeaNode getOriginalDef() {
		return originalDef;
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of();
	}

	@Override
	public IkeaBløck getBlock() {
		return bløck;
	}
}
