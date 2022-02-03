package com.github.firmwehr.gentle.backend.lego;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoPhi;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoRet;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An inter-block graph of lego nodes representing a firm Graph.
 */
@SuppressWarnings("UnstableApiUsage")
public class LegoGraph {

	private int idCounter;
	private final MutableNetwork<LegoNode, LegoEdge> network;

	public LegoGraph() {
		this.network = NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
	}

	/**
	 * Returns all input nodes (i.e. nodes this node depends on; arguments).
	 *
	 * @param node the node to get the inputs for
	 *
	 * @return all inputs for the node
	 */
	public List<LegoNode> getInputs(LegoNode node) {
		return network.outEdges(node).stream().sorted().map(LegoEdge::dst).toList();
	}

	public Set<LegoEdge> getInputEdges(LegoNode node) {
		return Collections.unmodifiableSet(network.outEdges(node));
	}

	/**
	 * Returns all output nodes (i.e. nodes that depend on this node; users).
	 *
	 * @param node the node to get the users for
	 *
	 * @return all users of this node
	 */
	public Set<LegoNode> getOutputs(LegoNode node) {
		return network.inEdges(node).stream().map(LegoEdge::src).collect(Collectors.toSet());
	}

	public Set<LegoEdge> getOutputEdges(LegoNode node) {
		return Collections.unmodifiableSet(network.inEdges(node));
	}

	/**
	 * Sets the input of a node at the given index to the passed value.
	 *
	 * @param node the node to set the input for
	 * @param index the index to set it at
	 * @param input the parent node to set it to
	 */
	public void setInput(LegoNode node, int index, LegoNode input) {
		// Delete existing edge
		LegoEdge oldEdge = null;
		for (LegoEdge edge : getInputEdges(node)) {
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

		network.addEdge(node, input, new LegoEdge(node, input, index));
	}

	/**
	 * Adds a node with all its parents.
	 *
	 * @param node the node to add
	 * @param inputs the inputs of the node
	 */
	public void addNode(LegoNode node, List<LegoNode> inputs) {
		if (network.nodes().contains(node)) {
			throw new InternalCompilerException("Tried to add a node twice: " + node);
		}
		network.addNode(node);
		for (int i = 0; i < inputs.size(); i++) {
			network.addEdge(node, inputs.get(i), new LegoEdge(node, inputs.get(i), i));
		}
	}

	/**
	 * Removes a node and all incident edges.
	 * <p>
	 * <em>Also removes the node from its block</em>
	 *
	 * @param node the node to remove
	 */
	public void removeNode(LegoNode node) {
		network.removeNode(node);
		node.block().nodes().remove(node);
	}

	/**
	 * Overwrites an existing phi to materialize its inputs.
	 *
	 * @param phi the phi node to overwrite
	 * @param inputs the phi inputs
	 */
	public void overwritePhi(LegoPhi phi, List<LegoNode> inputs) {
		if (!network.nodes().contains(phi)) {
			throw new InternalCompilerException("Phi was not already part of graph");
		}
		if (!getInputs(phi).isEmpty()) {
			throw new InternalCompilerException("Phi has inputs already");
		}
		for (int i = 0; i < inputs.size(); i++) {
			network.addEdge(phi, inputs.get(i), new LegoEdge(phi, inputs.get(i), i));
		}
	}

	/**
	 * Overwrites an existing return node to account for callee-saved values.
	 *
	 * @param ret the return node to overwrite
	 * @param inputs the return inputs
	 */
	public void overwriteRet(LegoRet ret, List<LegoNode> inputs) {
		if (!network.nodes().contains(ret)) {
			throw new InternalCompilerException("Return was not already part of graph");
		}
		if (getInputs(ret).size() > 1) {
			throw new InternalCompilerException("Return has too many inputs already");
		}
		for (int i = 0; i < inputs.size(); i++) {
			network.addEdge(ret, inputs.get(i), new LegoEdge(ret, inputs.get(i), i));
		}
	}

	public int nextId() {
		return idCounter++;
	}

	/**
	 * Represents an edge between two nodes.
	 *
	 * @param src the source of the edge, i.e. the node needing the data
	 * @param dst the destination of the edge, i.e. the node providing the data
	 * @param index the index on the source side
	 */
	public record LegoEdge(
		LegoNode src,
		LegoNode dst,
		int index
	) implements Comparable<LegoEdge> {

		@Override
		public int compareTo(LegoEdge o) {
			return Integer.compare(index, o.index);
		}
	}
}

