package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;

import java.util.List;

public interface IkeaNode {

	IkeaBøx box();

	List<IkeaNode> parents();

}
