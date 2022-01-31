package com.github.firmwehr.gentle.backend.ir.nodes;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.backend.ir.IkeaGraph;
import com.github.firmwehr.gentle.backend.ir.register.IkeaRegisterRequirement;
import com.github.firmwehr.gentle.backend.ir.register.X86Register;
import com.github.firmwehr.gentle.backend.ir.visit.IkeaVisitor;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Common base class for all Ikea nodes
 */
public abstract class IkeaNode {

	private final int id;
	private final IkeaBløck block;
	private final IkeaGraph graph;
	private final IkeaBøx.IkeaRegisterSize size;
	private final List<Node> firmNodes;
	private Optional<X86Register> register = Optional.empty();

	/**
	 * @param id The id of the node.
	 * @param block The parent block of the node.
	 * @param graph The associated graph.
	 * @param size The register size of the resulting value.
	 * @param firmNodes A list of firm nodes that are part of this ikea node.
	 */
	protected IkeaNode(int id, IkeaBløck block, IkeaGraph graph, IkeaBøx.IkeaRegisterSize size, List<Node> firmNodes) {
		this.id = id;
		this.block = block;
		this.graph = graph;
		this.size = size;
		this.firmNodes = List.copyOf(firmNodes);
	}

	@Override
	public final boolean equals(Object o) {
		return this == o;
	}

	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	// TODO: final can be removed, once refactoring is complete
	@Override
	public final String toString() {
		return display();
	}

	/**
	 * @return Currently assigned register.
	 */
	public final Optional<X86Register> register() {
		return register;
	}

	/**
	 * @param register The new target register.
	 */
	public final void register(X86Register register) {
		if (this.registerIgnore()) {
			throw new InternalCompilerException("Tried to assign register to regignore value");
		}
		this.register = Optional.of(register);
	}

	/**
	 * Fetches the target register by unwarpping the containing Optional.
	 *
	 * @return Result register
	 *
	 * @throws InternalCompilerException If no register has been assigned.
	 */
	public final X86Register uncheckedRegister() {
		return register.orElseThrow(
			() -> new InternalCompilerException("Expected register for " + this + " to be present!"));
	}

	/**
	 * Shortcut for {@code uncheckedRegister().nameForSize(node)}
	 *
	 * @return The x86 register name of this node, if it has a register.
	 *
	 * @throws InternalCompilerException If this node has not been assigned a register.
	 */
	public final String asRegisterName() {
		return uncheckedRegister().nameForSize(this);
	}

	/**
	 * @return true if this node should be ignored for register allocation
	 */
	public final boolean registerIgnore() {
		return registerRequirement().limitedTo().isEmpty();
	}

	/**
	 * I don't know, u tell me
	 *
	 * @return
	 */
	public boolean isTuple() {
		return false;
	}

	/**
	 * @return Internal id in ikea graph.
	 */
	public final int id() {
		return id;
	}

	/**
	 * @return Parent block.
	 */
	public final IkeaBløck block() {
		return block;
	}

	/**
	 * @return Containing ikea graph.
	 */
	public final IkeaGraph graph() {
		return graph;
	}

	/**
	 * @return Size of resulting value.
	 */
	public final IkeaBøx.IkeaRegisterSize size() {
		return size;
	}

	public final List<Node> underlyingFirmNodes() {
		return firmNodes;
	}

	public final List<IkeaNode> inputs() {
		return graph().getInputs(this);
	}

	public abstract <T> T accept(IkeaVisitor<T> visitor);

	// TODO: this can probably be done way better with subclass
	public final List<IkeaNode> results() {
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

	/**
	 * Used by VCG graph printing.
	 *
	 * @return Human readable string vor VCG graph.
	 */
	public String display() {
		return getClass().getSimpleName() + " (" + id() + ")";
	}

	/**
	 * @return Set of registers that will be clobbered by this operation.
	 */
	public Set<X86Register> clobbered() {
		return Set.of();
	}

	// TODO: explain
	public abstract List<IkeaRegisterRequirement> inRequirements();

	// TODO: explain
	public abstract IkeaRegisterRequirement registerRequirement();
}
