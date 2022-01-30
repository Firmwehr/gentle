package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx.IkeaRegisterSize;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaReload extends IkeaNode {

	private int spillSlot;
	private final IkeaNode originalValue;

	public IkeaReload(
		int id, IkeaBløck block, IkeaGraph graph, IkeaRegisterSize size, List<Node> firmNodes, IkeaNode originalValue
	) {
		super(id, block, graph, size, firmNodes);
		this.originalValue = originalValue;
	}

	public int spillSlot() {
		return spillSlot;
	}

	public void spillSlot(int spillSlot) {
		this.spillSlot = spillSlot;
	}

	public IkeaNode originalValue() {
		return originalValue;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		// Spill is parent but has no reg
		return List.of();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.gpRegister();
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " " + spillSlot + " (" + id() + ")";
	}
}
