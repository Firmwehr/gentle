package com.github.firmwehr.gentle.firm.model;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.GentleBindings;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import firm.Graph;
import firm.nodes.Node;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class LoopTree {
	private final MutableGraph<LoopElement> tree;
	private final LoopElementLoop root;

	private LoopTree(FirmIrLoop root) {
		this.root = new LoopElementLoop(0, root);
		this.tree = GraphBuilder.directed().allowsSelfLoops(false).build();

		buildRecursive(this.root);
	}

	public static LoopTree forGraph(Graph graph) {
		GentleBindings.assure_loopinfo(graph.ptr);

		return new LoopTree(FirmIrLoop.createWrapper(GentleBindings.get_irg_loop(graph.ptr)));
	}

	private void buildRecursive(LoopElementLoop root) {
		FirmIrLoop firmRoot = root.firmLoop();

		for (int i = 0; i < firmRoot.getLoopElementCount(); i++) {
			Object element = firmRoot.getElement(i);

			if (element instanceof FirmIrLoop child) {
				LoopElementLoop childLoop = new LoopElementLoop(root.depth() + 1, child);
				tree.putEdge(root, childLoop);

				buildRecursive(childLoop);
			} else if (element instanceof Node child) {
				tree.putEdge(root, new LoopElementNode(root.depth() + 1, child));
			} else {
				throw new InternalCompilerException("Unknown loop element " + element);
			}
		}
	}

	public LoopElementLoop getRoot() {
		return root;
	}

	/**
	 * Returns all child elements (loops or nodes) of a given parent.
	 *
	 * @param parent the parent loop
	 *
	 * @return all children
	 */
	public Set<LoopElement> getChildren(LoopElement parent) {
		return tree.successors(parent);
	}

	/**
	 * Returns the parent of a given loop element, if any exists.
	 *
	 * @param child the child to get the parent for
	 *
	 * @return the parent if the child is not the root
	 */
	public Optional<LoopElement> getParent(LoopElement child) {
		Set<LoopElement> predecessors = tree.predecessors(child);

		if (predecessors.isEmpty()) {
			return Optional.empty();
		}

		if (predecessors.size() > 1) {
			throw new InternalCompilerException(child + " has more than one parent!");
		}

		return Optional.of(predecessors.iterator().next());
	}

	private String toStringElement(LoopElement element) {
		return switch (element) {
			case LoopElementNode node -> node.toString();
			case LoopElementLoop node -> {
				String result = node + "\n";
				result += getChildren(element).stream()
					.map(this::toStringElement)
					.collect(Collectors.joining("\n"))
					.indent(2)
					.stripTrailing();

				yield result;
			}
		};
	}

	@Override
	public String toString() {
		return """
			LoopTree {
			%s
			}""".formatted(toStringElement(root).indent(2).stripTrailing());
	}

	/**
	 * An element in the loop tree. We only know it has a depth, but not whether it is a nested loop or instead a
	 * terminating node.
	 */
	public sealed interface LoopElement permits LoopElementLoop, LoopElementNode {
		int depth();
	}

	/**
	 * A {@link LoopElement} that represents a nested loop.
	 */
	public record LoopElementLoop(
		int depth,
		FirmIrLoop firmLoop
	) implements LoopElement {
	}

	/**
	 * A {@link LoopElement} that represents a node in a loop.
	 */
	public record LoopElementNode(
		int depth,
		Node firmNode
	) implements LoopElement {

	}
}
