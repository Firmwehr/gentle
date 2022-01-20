package com.github.firmwehr.gentle.backend.ir.nodes;


import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Load;
import firm.nodes.Node;

import java.util.List;
import java.util.stream.Stream;

public class IkeaMovLoadEx implements IkeaNode {

	private IkeaBøx box;
	private final IkeaBløck bløck;
	private final Load node;
	private final BoxScheme scheme;

	public IkeaMovLoadEx(IkeaBøx box, Load node, BoxScheme scheme, IkeaBløck bløck) {
		this.box = box;
		this.bløck = bløck;
		this.node = node;
		this.scheme = scheme;
	}

	public BoxScheme getScheme() {
		return scheme;
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

	@Override
	public IkeaBløck getBlock() {
		return bløck;
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}
}
