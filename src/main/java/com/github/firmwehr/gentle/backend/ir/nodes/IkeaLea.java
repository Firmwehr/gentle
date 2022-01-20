package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaLea implements IkeaNode {
	private final IkeaBøx register;
	private final List<IkeaNode> parents;
	private final IkeaBløck block;

	public IkeaLea(IkeaBøx register, List<IkeaNode> parents, IkeaBløck block) {
		this.register = register;
		this.parents = parents;
		this.block = block;
	}

	@Override
	public IkeaBøx box() {
		return register;
	}

	@Override
	public List<IkeaNode> parents() {
		return parents;
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
		return List.of(IkeaRegisterRequirement.gpRegister(), IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}
}
