package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.Collections;
import java.util.List;

public class IkeaCopy implements IkeaNode {
	private final IkeaBøx box;
	private final IkeaNode source;
	private final IkeaBløck block;

	public IkeaCopy(IkeaBøx box, IkeaNode source, IkeaBløck block) {
		this.box = box;
		this.source = source;
		this.block = block;
	}

	@Override
	public IkeaBøx box() {
		return box;
	}

	@Override
	public List<IkeaNode> parents() {
		return Collections.emptyList();
	}

	public IkeaNode getSource() {
		return source;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of();
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
		return List.of(IkeaRegisterRequirement.gpRegister());
	}
}