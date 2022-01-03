package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.nodes.Phi;

import java.util.List;

public class IkeaPhi implements IkeaNode {
	private IkeaBøx box;
	private final Phi phi;

	public IkeaPhi(IkeaBøx box, Phi phi) {
		this.box = box;
		this.phi = phi;
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}
}
