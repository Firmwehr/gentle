package com.github.firmwehr.gentle.backend.ir;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An inter-block graph of ikea nodes representing a firm Graph.
 */
@SuppressWarnings("UnstableApiUsage")
public class IkeaGraph {

	private final MutableNetwork<IkeaNode, IkeaEdge> network;

	public IkeaGraph() {
		this.network = NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
	}

	/**
	 * Returns all input nodes (i.e. nodes this node depends on; arguments).
	 *
	 * @param node the node to get the inputs for
	 *
	 * @return all inputs for the node
	 */
	public List<IkeaNode> getInputs(IkeaNode node) {
		return network.outEdges(node).stream().sorted().map(IkeaEdge::dst).toList();
	}

	public Set<IkeaEdge> getInputEdges(IkeaNode node) {
		return network.outEdges(node);
	}

	/**
	 * Returns all output nodes (i.e. nodes that depend on this node; users).
	 *
	 * @param node the node to get the users for
	 *
	 * @return all users of this node
	 */
	public Set<IkeaNode> getOutputs(IkeaNode node) {
		return network.inEdges(node).stream().map(IkeaEdge::src).collect(Collectors.toSet());
	}

	public Set<IkeaEdge> getOutputEdges(IkeaNode node) {
		return network.inEdges(node);
	}

	/**
	 * Sets the input of a node at the given index to the passed value.
	 *
	 * @param node the node to set the input for
	 * @param index the index to set it at
	 * @param input the parent node to set it to
	 */
	public void setInput(IkeaNode node, int index, IkeaNode input) {
		// Delete existing edge
		IkeaEdge oldEdge = null;
		for (IkeaEdge edge : getInputEdges(node)) {
			if (edge.index == index) {
				oldEdge = edge;
				break;
			}
		}

		if (oldEdge == null) {
			// TODO: Is this helpful?
			throw new InternalCompilerException(
				"You can not set an input if the node does not have an existing edge with that index");
		}
		network.removeEdge(oldEdge);

		network.addEdge(node, input, new IkeaEdge(node, input, index));
	}

	/**
	 * Adds a node with all its parents.
	 *
	 * @param node the node to add
	 * @param inputs the inputs of the node
	 */
	public void addNode(IkeaNode node, List<IkeaNode> inputs) {
		if (network.nodes().contains(node)) {
			throw new InternalCompilerException("Tried to add a node twice: " + node);
		}
		network.addNode(node);
		for (int i = 0; i < inputs.size(); i++) {
			network.addEdge(node, inputs.get(i), new IkeaEdge(node, inputs.get(i), i));
		}
	}

	/**
	 * Represents an edge between two nodes.
	 *
	 * @param src the source of the edge, i.e. the node needing the data
	 * @param dst the destination of the edge, i.e. the node providing the data
	 * @param index the index on the source side
	 */
	public record IkeaEdge(
		IkeaNode src,
		IkeaNode dst,
		int index
	) implements Comparable<IkeaEdge> {

		@Override
		public int compareTo(IkeaEdge o) {
			return Integer.compare(index, o.index);
		}
	}
}

