package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.asciiart.generating.BaseMatch;
import com.github.firmwehr.fiascii.generated.MatchBaseDisplacement;
import com.github.firmwehr.fiascii.generated.MatchBaseIndex;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexDisplacement;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexScale;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexScaleDisplacement0;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Pair;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This class extracts subtrees which can be represented via x86 address modes. Most reasoning is based on this website:
 * https://blog.yossarian.net/2020/06/13/How-x86_64-addresses-memory
 * <p>
 * Given the Minijava language specification and some other considerations, only a few of the available addressing modes
 * will actually be relevant for our compiler. The following list provides a description of all considered notes and
 * descriptions on which of them are supported by this class and which ones are not (and why).
 * <ul>
 *     <li>Base + Index <strong>[supported]</strong>: Needed for accessing boolean arrays</li>
 *     <li>Base + Index + Displacement <strong>[supported]</strong>: Models access to array of struct fields</li>
 *     <li>Base + (Index * Scale) <strong>[supported]</strong>: Array access</li>
 *     <li>Base + (Index * Scale) + Displacement <strong>[supported]</strong>: Constant folding may generate this</li>
 *     <li>Base + Displacement <strong>[supported]</strong>: Struct access pattern</li>
 *     <li>Displacement <strong>[nonsense]</strong>: We don't generate static objects</li>
 *     <li>Base <strong>[nonsense]</strong>: Will be eliminated by register allocator</li>
 *     <li>(Index * Scale) + Displacement <strong>[nonsense]</strong>: We don't have static array access</li>
 * </ul>
 */
public class CodePreselectionMatcher implements CodePreselection {

	private static final Logger LOGGER = new Logger(CodePreselectionMatcher.class, Logger.LogLevel.DEBUG);
	private static final List<MatchHandler<?>> MATCH_HANDLERS;

	static {
		// ACHTUNG! Ze order is weri importent! We start wis ze big tree metsch and muv downwerts
		// @formatter:off
		MATCH_HANDLERS = List.of(
			new MatchHandler<>(
				"base + (index * scale) + displacement (variant 0)",
				CodePreselectionMatcher::matchBaseIndexScaleDisplacement0,
				CodePreselectionMatcher::handleMatchBaseIndexScaleDisplacement0),
			new MatchHandler<>(
				"base + (index * scale)",
				CodePreselectionMatcher::matchBaseIndexScale,
				CodePreselectionMatcher::handleMatchBaseIndexScale),
			new MatchHandler<>(
				"base + displacement",
				CodePreselectionMatcher::matchBaseDisplacement,
				CodePreselectionMatcher::handleMatchBaseDisplacement),
			new MatchHandler<>(
				"base + (index)",
				CodePreselectionMatcher::matchBaseIndex,
				CodePreselectionMatcher::handleMatchBaseIndex)
		);
		// @formatter:on
	}

	private final Graph graph;
	private final IdentityHashMap<MatchHandler<?>, Integer> matchCounter = new IdentityHashMap<>();


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

		// prepopulate counter so we even include matcher with no hits
		for (var handler : MATCH_HANDLERS) {
			matchCounter.put(handler, 0);
		}

		performPreselection();

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Code Preselection statistics for %s", graph);

			for (var handler : MATCH_HANDLERS) {
				LOGGER.debug("\t %s: %s hits", handler.name, matchCounter.get(handler));
			}
		}
	}

	public IdentityHashMap<MatchHandler<?>, Integer> getMatchCounter() {
		return matchCounter;
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

			var pair = handler.analyse(n, replaced);
			if (pair.isPresent()) {
				var addr = pair.get().first();
				var match = pair.get().second();
				groupedNodes.put(n, addr);
				replaced.addAll(match.markedNodes());

				//noinspection ConstantConditions
				matchCounter.compute(handler, (matchHandler, integer) -> integer + 1);
			}
		}
	}


	@FiAscii("""
		// Common char array indexing (effective scale of 0)
		          ┌───────────┐         ┌───────────┐
		          │ base: *   │         │ index: *  │
		          └──────┬────┘         └─────┬─────┘
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
	public static Optional<MatchBaseIndex.Match> matchBaseIndex(Node node) {
		return MatchBaseIndex.match(node);
	}

	public static AddressingScheme handleMatchBaseIndex(MatchBaseIndex.Match match) {
		return new AddressingScheme(Optional.of(match.base()), Optional.of(match.index()), 0, 0);
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
			match.displacement().getTarval().asLong());
	}

	@FiAscii("""
			One of three variants (Variant 0)
			We assume that constant folding has folded away all constant base variables
			
		┌─────────────┐     ┌──────────────┐  ┌────────────┐   ┌──────────────────────┐
		│  index: *   │     │ scale: Const │  │  base: *   │   │ displacement: Const  │
		└────┬────────┘     └──────┬───────┘  └──────┬─────┘   └──────┬───────────────┘
		     │                     │                 │                │
		     │                     │                 │                │
		     └──────────┬──────────┘                 └───────┬────────┘        When you hope no one noticed
		                │                                    │                 ⣿⣿⣿⣿⣿⣿⡿⣟⠻⠯⠭⠉⠛⠋⠉⠉⠛⠻⢿⣿⣿⣿⣿⣿⣿
		                │                                    │                 ⣿⣿⣿⣿⡽⠚⠉⠀⠀⠀⠀⠀⠀⠀⠀⣀⣀⣀⠀⠈⠙⢿⣿⣿⣿⣿
		                │           Yes, I build it          │                 ⣿⣿⠏⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣾⣿⣿⣿⣷⣦⡀⠶⣿⣿⣿⣿
		          ┌─────▼─────┐       by hand         ┌──────▼──────┐          ⣿⡏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⡆⢻⣿⣿⣿
		          │ *mul: Mul │                       │ *add1: Add  │          ⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣤⣻⣿⣯⣤⣹⣿⣿
		          └─────┬─────┘     come fight me     └──────┬──────┘          ⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⢿⣿⡇⠀⣿⢟⣿⡀⠟⢹⣿⣿
		                │                                    │                 ⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣷⣤⣤⣼⣿⣿⡄⢹⣿⣿
		                │                                    │                 ⣷⠀⠀⠀⠶⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⣿⣿⣿⣿⣿⣿⠛⠉⠈⢻⣿
		                └─────────────────┬──────────────────┘                 ⣿⣷⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠛⠋⠛⠛⠛⠀⠀⣤⣾⣿
		                                  │                                    ⣿⣿⣿⣷⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠉⠉⠉⠉⠛⠁⣰⣿⣿⣿
		                                  │                                    ⣿⣿⣿⣿⣿⣷⣦⣤⣤⣤⣤⣄⣀⣀⣀⣀⣀⣠⣤⣤⣤⣾⣿⣿⣿
		                                  │
		                                  │
		                          ┌───────▼─────┐           ┌────────────────┐
		                          │ *add0: Add  │           │mem: * ; +memory│
		                          └───────┬─────┘           └──────┬─────────┘
		                                  │                        │
		                                  └──────────┬─────────────┘
		                                             │
		                                   ┌─────────▼─────────┐
		                                   │ terminator: Load  │
		                                   └───────────────────┘
			""")
	public static Optional<MatchBaseIndexScaleDisplacement0.Match> matchBaseIndexScaleDisplacement0(Node node) {
		return MatchBaseIndexScaleDisplacement0.match(node);
	}

	public static AddressingScheme handleMatchBaseIndexScaleDisplacement0(MatchBaseIndexScaleDisplacement0.Match match) {
		return new AddressingScheme(Optional.of(match.base()), Optional.of(match.index()),
			match.scale().getTarval().asLong(), match.displacement().getTarval().asLong());
	}

	@FiAscii("""
		base + index + displacement
		                                     ┌────────────┐   ┌──────────────────────┐
		                                     │  base: *   │   │ displacement: Const  │
		                                     └──────┬─────┘   └──────┬───────────────┘
		                                            │                │
		                                            │                │
		                                            └───────┬────────┘
		                                                    │
		                                                    │
		          ┌─────────────┐                           │
		          │  index: *   │                    ┌──────▼──────┐
		          └────┬────────┘                    │ *add1: Add  │
		               │                             └──────┬──────┘
		               │                                    │
		               │                                    │
		               └─────────────────┬──────────────────┘
		                                 │
		                                 │
		                                 │
		                                 │
		                         ┌───────▼─────┐           ┌────────────────┐
		                         │ *add0: Add  │           │mem: * ; +memory│
		                         └───────┬─────┘           └──────┬─────────┘
		                                 │                        │
		                                 └──────────┬─────────────┘
		                                            │
		                                  ┌─────────▼─────────┐
		                                  │ terminator: Load  │
		                                  └───────────────────┘
		""")
	public static Optional<MatchBaseIndexDisplacement.Match> matchBaseIndexDisplacement(Node node) {
		return MatchBaseIndexDisplacement.match(node);
	}

	public static AddressingScheme handleMatchBaseIndexDisplacement(MatchBaseIndexDisplacement.Match match) {
		return new AddressingScheme(Optional.of(match.base()), Optional.of(match.index()), 0,
			match.displacement().getTarval().asLong());
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
			match.scale().getTarval().asLong(), 0);
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

		if ((scheme.displacement & 0x00000000FFFFFFFFL) != scheme.displacement) {
			return false;
		}

		return true;
	}

	private record MatchHandler<M extends BaseMatch>(
		String name,
		Function<Node, Optional<M>> matcher,
		Function<M, AddressingScheme> handler
	) {
		/**
		 * Checks for the configured match and calls the attached handler on success. Will also check if the match has
		 * any marked nodes that have already been selected for replacement and thous can't be matched a second time.
		 *
		 * @param node The node to check for a match on.
		 * @param replaced The current set of replaced nodes. Nodes that have been replaced can no longer be replaced
		 * 	by a different op.
		 *
		 * @return A pair consisting of the found match and the addressing scheme or {@code none} if no such match was
		 * 	found.
		 */
		public Optional<Pair<AddressingScheme, BaseMatch>> analyse(Node node, Set<Node> replaced) {
			var maybeMatch = matcher.apply(node);
			return maybeMatch.flatMap(matcher -> {
				var match = maybeMatch.get();
				var scheme = handler.apply(match);

				// check if marked node has already been marked by previous match
				for (var marked : match.markedNodes()) {
					if (replaced.contains(marked)) {
						return Optional.empty();
					}
				}

				// only marked nodes will be folded into addressing scheme, so we only need to check them
				if (!isFreeFromExternalDependencies(match::markedNodes)) {
					LOGGER.debug("rejecting match [%s] due to external dependencies: %s", name, match);
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
		long scale,
		long displacement
	) {
		public Stream<Node> stream() {
			return Stream.concat(base.stream(), index.stream());
		}
	}
}
