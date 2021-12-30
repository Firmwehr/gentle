package com.github.firmwehr.gentle.backend.ir.visit;

import com.github.firmwehr.gentle.backend.ir.nodes.IkeaAdd;

public class MolkiVisitor {

	public void visit(IkeaAdd add) {
		System.out.println("add [" + "] -> " + add.box().assemblyName());
	}
}
