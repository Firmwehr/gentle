package com.github.firmwehr.gentle.backend.ir;

import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Block;

import java.util.List;

// Comes with a manual
public record IkeaBløck(
	List<IkeaParentBløck> parents,
	List<IkeaNode> nodes,
	Block origin
) {

	/**
	 * @return already in schedule order
	 */
	@Override
	public List<IkeaNode> nodes() {
		return nodes;
	}

	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public int id() {
		return origin.getNr();
	}
}
