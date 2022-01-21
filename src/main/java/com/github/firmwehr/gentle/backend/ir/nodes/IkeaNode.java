package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.RegisterInformation;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface IkeaNode {

	default RegisterInformation register() {
		// FIXME: Properly implement it
		return new RegisterInformation();
	}

	// TODO: Remove
	IkeaBøx box();

	List<IkeaNode> parents();

	<T> T accept(IkeaVisitor<T> visitor);

	List<Node> getUnderlyingFirmNodes();

	IkeaBløck getBlock();

	List<IkeaRegisterRequirement> inRequirements();

	default Set<X86Register> clobbered() {
		return outRequirements().stream().flatMap(it -> it.limitedTo().stream()).collect(Collectors.toSet());
	}

	List<IkeaRegisterRequirement> outRequirements();

	default IkeaRegisterRequirement regRequirement() {
		if (outRequirements().size() != 1) {
			throw new InternalCompilerException("More than one out req");
		}
		return outRequirements().get(0);
	}
}
