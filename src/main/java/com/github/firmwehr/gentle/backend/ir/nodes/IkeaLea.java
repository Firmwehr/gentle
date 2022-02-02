package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaLea implements IkeaNode {
	private final IkeaBøx register;
	private final List<IkeaNode> parents;

	public IkeaLea(IkeaBøx register, List<IkeaNode> parents) {
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

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of();
	}

}
