package com.github.firmwehr.gentle.backend.ir;

import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;

import java.util.Set;

public record IkeaParentBløck(
	IkeaBløck parent,
	Set<IkeaNode> parentNodes
) {
}
