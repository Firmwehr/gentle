package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.asciiart.generating.BaseMatch;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexScale;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Pair;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class CodePreselectionMatcher implements CodePreselection {

	private static final Logger LOGGER = new Logger(CodePreselectionMatcher.class);

	private final Graph graph;

	private static final List<MatchHandler<?>> MATCH_HANDLERS;

	static {
		MATCH_HANDLERS = List.of(new MatchHandler<>(CodePreselectionMatcher::matchBaseIndexScale,
			CodePreselectionMatcher::handleMatchBaseIndexScale));
	}

	/**
	 * Map terminating nodes from matched subtrees to their representing IkeaNode.
	 */
	private final Map<Node, AddressingScheme> groupedNodes = new HashMap<>();

	/**
	 * Keeps track of nodes that have been part of a aggregation operation.
	 */
	private final Set<Node> replaced = new HashSet<>();

	public CodePreselectionMatcher(Graph graph) {
		this.graph = graph;

		performPreselection();
	}

	private void performPreselection() {
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void defaultVisit(Node n) {
				CodePreselectionMatcher.this.defaultVisit(n);
			}
		});
		BackEdges.disable(graph);

		LOGGER.info("found %s address scheme opportunities covering %s nodes", groupedNodes.size(), replaced.size());
	}

	/**
	 * Checks and retrieves if the given node can be replaced with an addressing scheme.
	 *
	 * @param n The node to check.
	 *
	 * @return Optional addressing scheme.
	 */
	@Override
	public Optional<AddressingScheme> scheme(Node n) {
		return Optional.ofNullable(groupedNodes.get(n));
	}

	/**
	 * Checks if the given node has been replaced with an addressing scheme.
	 *
	 * @param n The node to check.
	 *
	 * @return {@code true} if node has been replaced.
	 */
	@Override
	public boolean hasBeenReplaced(Node n) {
		return replaced.contains(n);
	}

	private void defaultVisit(Node n) {
		for (var handler : MATCH_HANDLERS) {
			var pair = handler.analyse(n);
			if (pair.isPresent()) {
				var addr = pair.get().first();
				var match = pair.get().second();
				groupedNodes.put(n, addr);
				replaced.addAll(match.markedNodes());
			}
		}
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
		          ┌─────▼─────┐         ┌──────────┐
		          │ *mul: Mul │         │ base: *  │
		          └──────┬────┘         └─────┬────┘
		                 │                    │
		                 │                    │
		                 └──────┬─────────────┘
		                        │
		                        │
		                ┌───────▼─────┐          ┌────────────────┐
		                │  *add: Add  │          │mem: * ; +memory│
		                └──────┬──────┘          └──────┬─────────┘
		                       │                        │
		                       └──────────┬─────────────┘
		                                  │
		                            ┌─────▼─────────────┐
		                            │ *terminator: Load │
		                            └───────────────────┘
		""")
	public static Optional<MatchBaseIndexScale.Match> matchBaseIndexScale(Node node) {
		var maybeMatch = MatchBaseIndexScale.match(node);
		if (maybeMatch.isPresent()) {
			var match = maybeMatch.get();
			var val = match.scale().getTarval().asLong();
			if (val != 1 && val != 2 && val != 4 && val != 8) {
				LOGGER.debug("rejection match with invalid scale of %s: %s", val, match);
				return Optional.empty();
			}

			if (!isFreeFromExternalDependencies(match::markedNodes, match.terminator())) {
				LOGGER.debug("rejecting match due to external dependencies: %s", match);
			}
		}
		return maybeMatch;
	}

	public static AddressingScheme handleMatchBaseIndexScale(MatchBaseIndexScale.Match match) {
		return new AddressingScheme(match.base(), match.index(), match.scale().getTarval().asInt(), 0);
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

	private record MatchHandler<M extends BaseMatch>(
		Function<Node, Optional<M>> matcher,
		Function<M, AddressingScheme> handler
	) {
		public Optional<Pair<AddressingScheme, BaseMatch>> analyse(Node node) {
			var maybeMatch = matcher.apply(node);
			return maybeMatch.map(matcher -> {
				var match = maybeMatch.get();
				return new Pair<>(handler.apply(match), match);
			});
		}
	}

	/**
	 * This represents all x86 addressing modes. The effective address is of the form: {@code base + (index * scale) +
	 * displacement}.
	 */
	public record AddressingScheme(
		Node base,
		Node index,

		// power of 2 from 0-8
		int scale,

		// must be 32 bit constant
		long displacement
	) {
	}
}
