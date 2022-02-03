package com.github.firmwehr.gentle.backend.lego;

import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Block;

import java.util.List;

// Comes with a manual
public record LegoPlate(
	List<LegoParentBløck> parents,
	List<LegoNode> nodes,
	Block origin
) {

	/**
	 * @return already in schedule order
	 */
	@Override
	public List<LegoNode> nodes() {
		return nodes;
	}

	public <T> T accept(LegoVisitor<T> visitor) {
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
		LegoPlate legoBløck = (LegoPlate) o;
		return legoBløck.id() == id();
	}

	@Override
	public String toString() {
		return "LegoBløck{" + "origin=" + origin + '}';
	}
}
