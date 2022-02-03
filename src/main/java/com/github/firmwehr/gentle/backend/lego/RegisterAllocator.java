package com.github.firmwehr.gentle.backend.lego;

import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;

/**
 * This interface is not flexible enough to work with more advanced register allocators but I have no idea what they
 * will look like right now anways.
 */
public interface RegisterAllocator {

	LegoBøx getRegister(LegoNode node);
}
