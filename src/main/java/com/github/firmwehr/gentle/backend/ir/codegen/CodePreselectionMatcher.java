package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.asciiart.generating.BaseMatch;
import com.github.firmwehr.fiascii.generated.MatchBaseDisplacement;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CodePreselectionMatcher implements CodePreselection {

	private static final Logger LOGGER = new Logger(CodePreselectionMatcher.class);

	private final Graph graph;

	private static final List<MatchHandler<?>> MATCH_HANDLERS;

	static {
		// ACHTUNG! Ze order is weri importent! We start wis ze big tree metsch and muv downwerts
		// @formatter:off
		MATCH_HANDLERS = List.of(
			new MatchHandler<>(CodePreselectionMatcher::matchBaseIndexScale,
				CodePreselectionMatcher::handleMatchBaseIndexScale),
			new MatchHandler<>(CodePreselectionMatcher::matchBaseDisplacement,
				CodePreselectionMatcher::handleMatchBaseDisplacement)
		);
		// @formatter:on
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
		// Common struct access pattern
		          ┌───────────┐         ┌──────────────────────┐
		          │ base: *   │         │ displacement: Const  │
		          └──────┬────┘         └─────┬────────────────┘
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
	public static Optional<MatchBaseDisplacement.Match> matchBaseDisplacement(Node node) {
		return MatchBaseDisplacement.match(node);
	}

	public static AddressingScheme handleMatchBaseDisplacement(MatchBaseDisplacement.Match match) {
		return new AddressingScheme(Optional.of(match.base()), Optional.empty(), 0,
			match.displacement().getTarval().asInt());
	}

	@FiAscii("""
		// Common array access pattern
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
		                            │ terminator: Load  │
		                            └───────────────────┘
		""")
	public static Optional<MatchBaseIndexScale.Match> matchBaseIndexScale(Node node) {
		return MatchBaseIndexScale.match(node);
	}

	public static AddressingScheme handleMatchBaseIndexScale(MatchBaseIndexScale.Match match) {
		return new AddressingScheme(Optional.of(match.base()), Optional.of(match.index()),
			match.scale().getTarval().asInt(), 0);
	}

	/**
	 * Checks if all marked nodes are free from external dependants. This ensures that we are not going to remove
	 * operations that other nodes still depend on.
	 *
	 * @param matchSupplier A supplied that will provide the methode with all nodes of the matched subtree.
	 *
	 * @return {@code true} if this match is free from external dependants or {@code false} if it isn't.
	 */
	private static boolean isFreeFromExternalDependencies(Supplier<Set<Node>> matchSupplier) {
		var match = matchSupplier.get();
		for (var n : match) {

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


	@SuppressWarnings({"RedundantIfStatement"})
	private static boolean isValidAddressingScheme(AddressingScheme scheme) {

		// explicit null check since matcher logic can easily be wired wrong by accident
		Objects.requireNonNull(scheme.base);
		Objects.requireNonNull(scheme.index);

		var scale = scheme.scale;
		if (scale != 1 && scale != 2 && scale != 4 && scale != 8) {
			return false;
		}

		// TODO: check if displacement is effectiv length of 33 bit (due to seperate signedness bit) I don't know
		if ((scheme.displacement & 0x00000000FFFFFFFFL) != scheme.displacement) {
			return false;
		}

		return true;
	}

	private record MatchHandler<M extends BaseMatch>(
		Function<Node, Optional<M>> matcher,
		Function<M, AddressingScheme> handler
	) {
		public Optional<Pair<AddressingScheme, BaseMatch>> analyse(Node node) {
			var maybeMatch = matcher.apply(node);
			return maybeMatch.flatMap(matcher -> {
				var match = maybeMatch.get();
				var scheme = handler.apply(match);

				// only marked nodes will be folded into addressing scheme, so we only need to check them
				if (!isFreeFromExternalDependencies(match::markedNodes)) {
					LOGGER.debug("rejecting match due to external dependencies: %s", match);
				}

				// filter out match that can't be represented in x86
				return isValidAddressingScheme(scheme) ? Optional.of(new Pair<>(scheme, match)) : Optional.empty();
			});
		}
	}

	/**
	 * This represents all (the more complex ones) x86 addressing modes. The effective address is of the form: {@code
	 * base + (index * scale) + displacement}.
	 *
	 * @param base Node that will produce the base value.
	 * @param index Node that will produce the index value.
	 * @param scale The scalar for index multiplication. Must be 0, 1, 2, 4, 8.
	 * @param displacement Total displacement. Must fit into 32bit signed integer.
	 */
	public record AddressingScheme(
		Optional<Node> base,
		Optional<Node> index,
		int scale,
		long displacement
	) {
		public Stream<Node> stream() {
			return Stream.concat(base.stream(), index.stream());
		}
	}
}
