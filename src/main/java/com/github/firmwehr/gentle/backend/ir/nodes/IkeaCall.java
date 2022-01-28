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
import java.util.Set;
import java.util.stream.IntStream;

public record IkeaCall(
	Mut<Optional<X86Register>> register,
	IkeaBløck block,
	IkeaGraph graph,
	List<Node> underlyingFirmNodes,
	int id
) implements IkeaNode {

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		List<X86Register> registerOrder = registerOrder();
		return IntStream.range(0, graph.getInputs(this).size())
			.mapToObj(registerOrder::get)
			.map(IkeaRegisterRequirement::singleRegister)
			.toList();
	}

	@Override
	public IkeaRegisterRequirement registerRequirement() {
		return IkeaRegisterRequirement.singleRegister(X86Register.RAX);
	}

	@Override
	public Set<X86Register> clobbered() {
		return Set.of(X86Register.R11, X86Register.R10, X86Register.R9, X86Register.R8, X86Register.RDI,
			X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.RAX, X86Register.RSP);
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	private List<X86Register> registerOrder() {
		return List.of(X86Register.RDI, X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.R8,
			X86Register.R9);
	}

	@Override
	public String toString() {
		return "IkeaCall";
	}
}
