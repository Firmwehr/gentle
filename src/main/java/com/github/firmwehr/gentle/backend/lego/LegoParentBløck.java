package com.github.firmwehr.gentle.backend.lego;

import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;

import java.util.Set;

public record LegoParentBløck(
	LegoPlate parent,
	Set<LegoNode> parentNodes
) {
}
