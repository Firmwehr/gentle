package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import com.github.firmwehr.gentle.util.Mut;
import firm.nodes.Node;

import java.util.List;
import java.util.Optional;

public record IkeaPhi(
	Mut<Optional<X86Register>> register,
	IkeaBløck block,
	IkeaGraph graph,
	List<Node> underlyingFirmNodes
) implements IkeaNode {

	@Override
	public <T> T accept(IkeaVisitor<T> visitor) {
		return visitor.visit(this);
	}

	public IkeaNode parent(IkeaBløck parentBlock) {
		List<IkeaParentBløck> parents = block.parents();
		for (int i = 0; i < parents.size(); i++) {
			if (parents.get(i).parent().equals(parentBlock)) {
				return graph.getInputs(this).get(i);
			}
		}
		throw new InternalCompilerException("Could not find parent block " + parentBlock);
	}

	@Override
	public List<IkeaRegisterRequirement> inRequirements() {
		return graph.getInputs(this).stream().map(IkeaNode::registerRequirement).toList();
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
		return getClass().getSimpleName();
	}
}
