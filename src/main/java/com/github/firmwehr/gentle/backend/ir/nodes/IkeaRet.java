package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;
import firm.nodes.Return;

import java.util.List;
import java.util.Optional;

public class IkeaRet implements IkeaNode {
	private final Optional<IkeaNode> value;
	private final Return firmReturn;
	private final IkeaBløck block;

	public IkeaRet(Optional<IkeaNode> value, Return firmReturn, IkeaBløck block) {
		this.value = value;
		this.firmReturn = firmReturn;
		this.block = block;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx(IkeaRegisterSize.ILLEGAL);
	}

	@Override
	public List<IkeaNode> parents() {
		return value.stream().toList();
	}

	public Optional<IkeaNode> getValue() {
		return value;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(firmReturn);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}
}
