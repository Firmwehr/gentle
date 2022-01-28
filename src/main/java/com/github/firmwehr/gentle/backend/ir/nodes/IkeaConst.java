package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import com.github.firmwehr.gentle.util.Mut;
import firm.TargetValue;
import firm.nodes.Node;

import java.util.List;
import java.util.Optional;

public record IkeaConst(
	Mut<Optional<X86Register>> register,
	IkeaBløck block,
	IkeaGraph graph,
	List<Node> underlyingFirmNodes,
	TargetValue value,
	int id
) implements IkeaNode {

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return List.of();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.gpRegister();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "IkeaConst " + value.asLong() + " (" + id() + ")";
	}
}
