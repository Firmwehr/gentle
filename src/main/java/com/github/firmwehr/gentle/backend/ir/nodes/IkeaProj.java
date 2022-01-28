package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import com.github.firmwehr.gentle.util.Mut;
import firm.nodes.Node;

import java.util.List;
import java.util.Optional;

public record IkeaProj(
	Mut<Optional<X86Register>> register,
	IkeaBløck block,
	IkeaGraph graph,
	List<Node> underlyingFirmNodes,
	int index,
	Mut<IkeaRegisterRequirement> regRequirement,
	int id
) implements IkeaNode {

	public IkeaProj(
		Mut<Optional<X86Register>> register, IkeaBløck block, IkeaGraph graph, List<Node> underlyingFirmNodes,
		int index, int id
	) {
		this(register, block, graph, underlyingFirmNodes, index, new Mut<>(IkeaRegisterRequirement.gpRegister()), id);
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
		return regRequirement.get();
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	public void setRegisterRequirement(IkeaRegisterRequirement requirement) {
		regRequirement.set(requirement);
	}

	@Override
	public String toString() {
		return "IkeaProj " + index;
	}
}
