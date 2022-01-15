package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;
import firm.nodes.Phi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IkeaPhi implements IkeaNode {
	private IkeaBøx box;
	private final Phi phi;
	private final Map<IkeaBløck, IkeaNode> parents;
	private final IkeaBløck block;

	public IkeaPhi(IkeaBøx box, Phi phi, IkeaBløck block) {
		this.box = box;
		this.phi = phi;
		this.block = block;
		this.parents = new HashMap<>();
	}

	public void addParent(IkeaNode node, IkeaBløck parent) {
		parents.put(parent, node);
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	public Map<IkeaBløck, IkeaNode> getParents() {
		return parents;
	}

	@Override
	public List<IkeaNode> parents() {
		return List.copyOf(parents.values());
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(phi);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}
}
