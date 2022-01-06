package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaUnassignedBøx;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Jmp;

import java.util.List;

public class IkeaJmp implements IkeaNode {
	private final IkeaBløck target;

	public IkeaJmp(IkeaBløck target, Jmp node) {
		this.target = target;
	}

	@Override
	public IkeaBøx box() {
		return new IkeaUnassignedBøx(IkeaRegisterSize.ILLEGAL);
	}

	@Override
	public List<IkeaNode> parents() {
		return List.of();
	}

	public IkeaBløck getTarget() {
		return target;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}
}
