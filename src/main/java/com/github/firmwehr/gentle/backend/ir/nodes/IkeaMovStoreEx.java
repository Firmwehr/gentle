package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;
import firm.nodes.Store;

import java.util.List;
import java.util.stream.Stream;

public class IkeaMovStoreEx implements IkeaNode {

	private final IkeaNode value;
	private final Store node;
	private final BoxScheme scheme;
	private final IkeaBløck block;

	public IkeaMovStoreEx(IkeaNode value, Store node, BoxScheme scheme, IkeaBløck block) {
		this.value = value;
		this.node = node;
		this.scheme = scheme;
		this.block = block;
	}

	public BoxScheme getScheme() {
		return scheme;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx(IkeaRegisterSize.ILLEGAL);
	}

	@Override
	public List<IkeaNode> parents() {
		return Stream.concat(scheme.base().stream(), scheme.index().stream()).toList();
	}

	public IkeaNode getValue() {
		return value;
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
		return block;
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		return List.of();
	}
}
