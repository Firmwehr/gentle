package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import com.github.firmwehr.gentle.util.Mut;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface IkeaNode {

	Mut<Optional<X86Register>> register();

	default boolean registerIgnore() {
		return registerRequirement().limitedTo().isEmpty();
	}

	default boolean isTuple() {
		return false;
	}

	int id();

	IkeaBløck block();

	IkeaGraph graph();

	List<Node> underlyingFirmNodes();

	<T> T accept(IkeaVisitor<T> visitor);

	default List<IkeaNode> inputs() {
		return graph().getInputs(this);
	}

	default List<IkeaNode> results() {
		List<IkeaNode> results = graph().getOutputs(this)
			.stream()
			.filter(it -> it instanceof IkeaProj)
			.collect(Collectors.toCollection(ArrayList::new));

		// We are not a tuple node (Div) so we need represent a result ourself!
		if (!isTuple()) {
			results.add(this);
		}

		return results;
	}

	List<IkeaRegisterRequirement> inRequirements();

	default Set<X86Register> clobbered() {
		return Set.of();
	}

	IkeaRegisterRequirement registerRequirement();
}
