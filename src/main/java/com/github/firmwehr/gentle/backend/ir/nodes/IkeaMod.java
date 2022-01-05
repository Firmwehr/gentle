package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import firm.nodes.Mod;

import java.util.List;

public class IkeaMod implements IkeaNode {
	private IkeaBøx box;
	private final IkeaNode left;
	private final IkeaNode right;
	private final Mod mod;

	public IkeaMod(IkeaBøx box, IkeaNode left, IkeaNode right, Mod mod) {
		this.box = box;
		this.left = left;
		this.right = right;
		this.mod = mod;
	}

	@Override
	public IkeaBøx box() {
		return this.box;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of(this.left, this.right);
	}
}
