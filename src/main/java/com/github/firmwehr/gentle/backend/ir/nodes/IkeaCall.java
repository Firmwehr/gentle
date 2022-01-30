package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public final class IkeaCall extends IkeaNode {

	public static final List<X86Register> REGISTER_ORDER =
		List.of(X86Register.RDI, X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.R8, X86Register.R9);

	public IkeaCall(
		int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes
	) {
		super(id, block, graph, size, firmNodes);
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return IntStream.range(0, graph().getInputs(this).size())
			.mapToObj(REGISTER_ORDER::get)
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
}
