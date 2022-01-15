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

	@Override
	public int hashCode() {
		return id();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IkeaBløck ikeaBløck = (IkeaBløck) o;
		return ikeaBløck.id() == id();
	}

	@Override
	public String toString() {
		return "IkeaBløck{" + "origin=" + origin + '}';
	}
}
