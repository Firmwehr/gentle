package com.github.firmwehr.gentle.backend.ir;

import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
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
}
