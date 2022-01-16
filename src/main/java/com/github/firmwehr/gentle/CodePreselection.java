package com.github.firmwehr.gentle;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.AdressModeTest;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.github.firmwehr.gentle.firm.optimization.GraphOptimizationStep;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class CodePreselection extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(CodePreselection.class);

	private final Graph graph;

	/**
	 * Map terminating nodes from matched subtrees to their representing IkeaNode.
	 */
	private final Map<Node, IkeaNode> groupedNodes = new HashMap<>();

	public CodePreselection(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> arithmeticOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("CodePreselection")
			.withOptimizationFunction(graph -> {
				BackEdges.enable(graph);

				CodePreselection codePreselection = new CodePreselection(graph);
				codePreselection.preselectCode();


				BackEdges.disable(graph);

				return false;
			})
			.build();
	}

	@FiAscii("""
		  ┌─────────────┐          ┌──────────────┐
		  │  index: *   │          │ scale: Const │
		  └────┬────────┘          └──────┬───────┘
		       │                          │
		       │                          │
		       └────────┬─────────────────┘
		                │
		                │
		                │
		          ┌─────▼─────┐          ┌─────────┐
		          │  mul: Mul │          │ base: * │
		          └──────┬────┘          └────┬────┘
		                 │                    │
		                 │                    │
		                 └──────┬─────────────┘
		                        │
		                        │
		                ┌───────▼──────┐         ┌────────────────┐
		                │add: Add      │         │mem: * ; +memory│
		                └──────┬───────┘         └──────┬─────────┘
		                       │                        │
		                       └──────────┬─────────────┘
		                                  │
		                             ┌────▼─────────────┐
		                             │ terminator: Load │
		                             └──────────────────┘
		""")
	public static Optional<AdressModeTest.Match> adressModeTest(Node node) {
		var maybeMatch = AdressModeTest.match(node);
		if (maybeMatch.isPresent()) {
			var match = maybeMatch.get();
			var val = match.scale().getTarval().asLong();
			if (val != 1 && val != 2 && val != 4 && val != 8) {
				LOGGER.debug("rejection match with invalid scale of %s: %s", val, match);
				return Optional.empty();
			}

			if (!isFreeFromExternalDependencies(match::matchedNodes, match.terminator())) {
				LOGGER.debug("rejecting match due to external dependencies: %s", match);
			}
		}
		return maybeMatch;
	}

	/**
	 * Checks if the given match is free from external connections, meaning that all nodes within the match will not be
	 * depended on from other nodes outside of the match. The terminator node is special, since it can (and should)
	 * have
	 * other nodes depending on it, as it will be the result of the aggregated operation.
	 *
	 * @param matchSupplier A supplied that will provide the methode with all nodes of the matched subtree.
	 * @param terminator Terminating node, that represents the final result of the aggregated computation.
	 *
	 * @return {@code true} if this match is free from external dependants or {@code false} if it isn't.
	 */
	private static boolean isFreeFromExternalDependencies(Supplier<Set<Node>> matchSupplier, Node terminator) {
		var match = matchSupplier.get();
		for (var n : match) {
			// terminating node is part of match but can be depended upon
			if (n.equals(terminator)) {
				continue;
			}

			// back edges for other nodes are only allowed inside match
			for (BackEdges.Edge edge : BackEdges.getOuts(n)) {
				var depender = edge.node;
				if (!match.contains(depender)) {
					return false;
				}
			}
		}
		return true;
	}

	private void preselectCode() {
		graph.walkTopological(this);
	}

	@Override
	public void defaultVisit(Node n) {
		var match = adressModeTest(n);
		match.ifPresent(value -> LOGGER.info("found match: %s", value));


	}
}
