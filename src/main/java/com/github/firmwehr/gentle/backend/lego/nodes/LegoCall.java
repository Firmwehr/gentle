package com.github.firmwehr.gentle.backend.lego.nodes;

import com.github.firmwehr.gentle.backend.lego.LegoBøx;
import com.github.firmwehr.gentle.backend.lego.LegoGraph;
import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.register.LegoRegisterRequirement;
import com.github.firmwehr.gentle.backend.lego.register.X86Register;
import com.github.firmwehr.gentle.backend.lego.visit.LegoVisitor;
import firm.Entity;
import firm.nodes.Node;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public final class LegoCall extends LegoNode {

	public static final List<X86Register> REGISTER_ORDER =
		List.of(X86Register.RDI, X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.R8, X86Register.R9);

	private final Entity entity;

	public LegoCall(
		int id, LegoPlate block, LegoGraph graph, LegoBøx.LegoRegisterSize size, List<Node> firmNodes, Entity entity
	) {
		super(id, block, graph, size, firmNodes);
		this.entity = entity;
	}

	public Entity entity() {
		return entity;
	}

	@Override
	public <T> T accept(LegoVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<LegoRegisterRequirement> inRequirements() {
		return IntStream.range(0, graph().getInputs(this).size())
			.mapToObj(REGISTER_ORDER::get)
			.map(LegoRegisterRequirement::singleRegister)
			.toList();
	}

	@Override
	public LegoRegisterRequirement registerRequirement() {
		if (size() != LegoBøx.LegoRegisterSize.ILLEGAL) {
			return LegoRegisterRequirement.singleRegister(X86Register.RAX);
		}
		return LegoRegisterRequirement.none();
	}

	@Override
	public Set<X86Register> clobbered() {
		return Set.of(X86Register.R11, X86Register.R10, X86Register.R9, X86Register.R8, X86Register.RDI,
			X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.RAX);
	}
}
