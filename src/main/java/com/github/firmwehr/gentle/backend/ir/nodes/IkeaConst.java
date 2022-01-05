package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Const;

import java.util.List;

public record IkeaConst(
	IkeaImmediate box,
	Const node
) implements IkeaNode {

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

}
