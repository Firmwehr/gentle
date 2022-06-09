package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.nodes.Node;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class LegoDiv extends LegoNode {

	private final boolean small;

	public LegoDiv(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes, boolean small
	) {
		super(id, block, graph, size, firmNodes);
		this.small = small;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public boolean isTuple() {
		return true;
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return List.of(LegoRegisterRequirement.singleRegister(X86Register.RAX), LegoRegisterRequirement.gpRegister());
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		return LegoRegisterRequirement.none();
	}

	@Override
	public Set<X86Register> clobbered() {
		return EnumSet.of(X86Register.RAX, X86Register.RDX);
	}

	@Override
	public String display() {
		return getClass().getSimpleName() + " (" + id() + ")";
	}

	public boolean small() {
		return this.small;
	}
}
