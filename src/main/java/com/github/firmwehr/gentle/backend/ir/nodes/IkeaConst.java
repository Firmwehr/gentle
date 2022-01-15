package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public record IkeaConst(
	IkeaImmediate box,
	Node node,
	IkeaBløck block
) implements IkeaNode {

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
		return List.of(node);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}
}
