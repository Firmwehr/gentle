package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public class IkeaSpill extends IkeaNode {

	private int spillSlot;

	public IkeaSpill(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	public int spillSlot() {
		return spillSlot;
	}

	public void spillSlot(int spillSlot) {
		this.spillSlot = spillSlot;
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of(IkeaRegisterRequirement.gpRegister());
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.none();
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " " + spillSlot + " (" + id() + ")";
	}
}
