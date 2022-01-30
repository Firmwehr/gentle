package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;

public final class IkeaProj extends IkeaNode {

	private final int index;
	private final String nameForIndex;

	private IkeaRegisterRequirement registerRequirement;

	public IkeaProj(
		int id,
		IkeaBløck block,
		IkeaGraph graph,
		IkeaBøx.IkeaRegisterSize size,
		List<Node> firmNodes,
		int index,
		String nameForIndex
	) {
		super(id, block, graph, size, firmNodes);
		this.index = index;
		this.nameForIndex = nameForIndex;
		this.registerRequirement = IkeaRegisterRequirement.gpRegister();
	}

	public String nameForIndex() {
		return nameForIndex;
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
		return registerRequirement;
	}

	public void registerRequirement(IkeaRegisterRequirement regRequirement) {
		this.registerRequirement = regRequirement;
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " " + index + ": " + nameForIndex() + " (" + id() + ")";
	}

	public int index() {
		return index;
	}
}
