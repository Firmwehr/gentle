package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Node;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public record IkeaCall(
	IkeaBøx box,
	Address address,
	List<IkeaNode> arguments,
	Call call,
	IkeaBløck block
) implements IkeaNode {

	@Override
	public List<IkeaNode> parents() {
		return arguments();
	}

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public List<Node> getUnderlyingFirmNodes() {
		return List.of(call);
	}

	@Override
	public IkeaBløck getBlock() {
		return block;
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return IntStream.range(0, arguments.size())
			.mapToObj(callRegisters()::get)
			.map(IkeaRegisterRequirement::singleRegister)
			.toList();
	}

	@Override
	public List<IkeaRegisterRequirement> outRequirements() {
		return List.of(IkeaRegisterRequirement.singleRegister(X86Register.RAX));
	}

	@Override
	public Set<X86Register> clobbered() {
		return Set.of(X86Register.R11, X86Register.R10, X86Register.R9, X86Register.R8, X86Register.RDI,
			X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.RAX, X86Register.RSP);
	}

	private List<X86Register> callRegisters() {
		return List.of(X86Register.RDI, X86Register.RSI, X86Register.RDX, X86Register.RCX, X86Register.R8,
			X86Register.R9);
	}
}
