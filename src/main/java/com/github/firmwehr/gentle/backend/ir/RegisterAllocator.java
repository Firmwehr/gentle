package com.github.firmwehr.gentle.backend.ir;

import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;

/**
 * This interface is not flexible enough to work with more advanced register allocators but I have no idea what they
 * will look like right now anways.
 */
public interface RegisterAllocator {

	IkeaBøx getRegister(IkeaNode node);
}
