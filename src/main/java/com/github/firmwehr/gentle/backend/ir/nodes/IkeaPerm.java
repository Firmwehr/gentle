package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class IkeaPerm implements IkeaNode {

	private final List<IkeaNode> inputs;
	private final IkeaBløck block;
	private final List<List<IkeaRegisterRequirement>> outRequirements;

	public IkeaPerm(List<IkeaNode> inputs, IkeaBløck block) {
		this.inputs = inputs;
		this.block = block;
		this.outRequirements = new ArrayList<>();
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx(IkeaBøx.IkeaRegisterSize.BITS_32);
	}

	@Override
	public List<IkeaNode> parents() {
		return inputs;
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
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of();
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		// TODO: What should we do here?
		return List.of();
	}

	public void setOutRequirements(List<List<IkeaRegisterRequirement>> requirements) {
		outRequirements.clear();
		outRequirements.addAll(requirements);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}
}
