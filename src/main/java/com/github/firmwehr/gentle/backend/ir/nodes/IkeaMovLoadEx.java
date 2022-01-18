package com.github.firmwehr.gentle.backend.ir.nodes;


import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Load;
import firm.nodes.Node;

import java.util.List;
import java.util.stream.Stream;

public class IkeaMovLoadEx implements IkeaNode {

	private IkeaBøx box;
	private final Load node;

	public BoxScheme getScheme() {
		return scheme;
	}

	private final BoxScheme scheme;

	public IkeaMovLoadEx(IkeaBøx box, Load node, BoxScheme scheme) {
		this.box = box;
		this.node = node;
		this.scheme = scheme;
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return Stream.concat(scheme.base().stream(), scheme.index().stream()).toList();
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(node);
	}
}
