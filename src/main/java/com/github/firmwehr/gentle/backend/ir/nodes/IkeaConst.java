package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaImmediate;

import java.util.List;

public record IkeaConst(IkeaImmediate box) implements IkeaNode {

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}

}
