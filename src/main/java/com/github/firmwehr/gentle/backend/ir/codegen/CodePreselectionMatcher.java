package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.asciiart.generating.BaseMatch;
import com.github.firmwehr.fiascii.generated.MatchBaseDisplacement;
import com.github.firmwehr.fiascii.generated.MatchBaseIndex;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexDisplacement0;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexDisplacement1;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexScale;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexScaleDisplacement0;
import com.github.firmwehr.fiascii.generated.MatchBaseIndexScaleDisplacement1;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Pair;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Const;
import firm.nodes.Load;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
				"base + (index * scale) + displacement (variant 1)",
				CodePreselectionMatcher::matchBaseIndexScaleDisplacement1,
				CodePreselectionMatcher::handleMatchBaseIndexScaleDisplacement1),
			new MatchHandler<>(
				"base + (index) + displacement (variant 0)",
				CodePreselectionMatcher::matchBaseIndexDisplacement0,
				CodePreselectionMatcher::handleMatchBaseIndexDisplacement0),
			new MatchHandler<>(
				"base + (index) + displacement (variant 1)",
				CodePreselectionMatcher::matchBaseIndexDisplacement1,
				CodePreselectionMatcher::handleMatchBaseIndexDisplacement1),
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

	/**
	 * Number of replaced (matched) subtrees across all addressing schemes.
	 */
	private int total;

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

	private void performPreselection() {
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void defaultVisit(Node n) {
				CodePreselectionMatcher.this.defaultVisit(n);
			}
		});
		BackEdges.disable(graph);
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

	@Override
	public int replacedSubtrees() {
		return total;
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
				total++;
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
		                            │  terminator: *    │
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
		                            │  terminator: *    │
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
			Variant 0
			We assume that constant folding has folded away all constant base variables
			
		┌─────────────┐     ┌──────────────┐  ┌────────────┐   ┌──────────────────────┐
		│  index: *   │     │ scale: Const │  │  base: *   │   │ displacement: Const  │
		└────┬────────┘     └──────┬───────┘  └──────┬─────┘   └──────┬───────────────┘
		     │                     │                 │                │
		     │                     │                 │                │        When everyone is writing code
		     └──────────┬──────────┘                 └───────┬────────┘        while you are creating ASCII art
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
		                                   │ terminator: *     │
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
		    Variant 1
		    This pattern checks for a different graph structure that performs the same calculation as variant 0
		    however, both the base and displacment could be swapped.
					
		    To reduce the amount of required graph matching calls, we don't restrict the pattern to a Const node but
		    instead perform the check in code by ourself
					
		┌─────────────┐     ┌──────────────┐
		│  index: *   │     │ scale: Const │
		└────┬────────┘     └──────┬───────┘
		     │                     │
		     │                     │
		     └──────────┬──────────┘
		                │
		                │
		                │
		          ┌─────▼─────┐                       ┌─────────────┐
		          │ *mul: Mul │                       │   x: *      │ This might be Const
		          └─────┬─────┘                       └──────┬──────┘
		                │                                    │
		                │                                    │
		                └─────────────────┬──────────────────┘
		                                  │
		                                  │
		                                  │
		      Or this might be const      │
		            ┌────────┐    ┌───────▼─────┐
		            │  y: *  │    │ *add1: Add  │            Const check is done in code
		            └────┬───┘    └───────┬─────┘
		                 │                │
		                 └───────┬────────┘
		                         │
		                 ┌───────▼─────┐           ┌────────────────┐
		                 │ *add0: Add  │           │mem: * ; +memory│
		                 └───────┬─────┘           └──────┬─────────┘
		                         │                        │
		                         └──────────┬─────────────┘
		                                    │
		                          ┌─────────▼─────────┐
		                          │ terminator: *     │
		                          └───────────────────┘
		    """)
	public static Optional<MatchBaseIndexScaleDisplacement1.Match> matchBaseIndexScaleDisplacement1(Node node) {
		var match = MatchBaseIndexScaleDisplacement1.match(node);

		return match.filter(m -> {
			// for this pattern to work either x xor y needs to be const
			return m.x() instanceof Const && !(m.y() instanceof Const) ||
				m.y() instanceof Const && !(m.x() instanceof Const);
		});
	}

	public static AddressingScheme handleMatchBaseIndexScaleDisplacement1(MatchBaseIndexScaleDisplacement1.Match match) {
		/*
		 * we have no way of carrying over the meaning of x and y, so we have to repeat the check, knowing that it's
		 * one or the other
		 */
		var base = match.x() instanceof Const ? match.y() : match.x();
		Const displacement = (Const) (match.x() instanceof Const ? match.x() : match.y());

		return new AddressingScheme(Optional.of(base), Optional.of(match.index()), match.scale().getTarval().asLong(),
			displacement.getTarval().asLong());
	}

	@FiAscii("""
		base + index + displacement (variant 0)
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
		                                  │ terminator: *     │
		                                  └───────────────────┘
		""")
	public static Optional<MatchBaseIndexDisplacement0.Match> matchBaseIndexDisplacement0(Node node) {
		return MatchBaseIndexDisplacement0.match(node);
	}

	public static AddressingScheme handleMatchBaseIndexDisplacement0(MatchBaseIndexDisplacement0.Match match) {
		return new AddressingScheme(Optional.of(match.base()), Optional.of(match.index()), 0,
			match.displacement().getTarval().asLong());
	}

	@FiAscii("""
		base + index + displacement (variant 1)
		                                     ┌────────────┐   ┌───────────┐
		                                     │  base: *   │   │ index: *  │
		                                     └──────┬─────┘   └──────┬────┘
		                                            │                │
		                                            │                │
		                                            └───────┬────────┘
		                                                    │
		                                                    │
		          ┌────────────────────────┐                │
		          │  displacement: Const   │         ┌──────▼──────┐
		          └────┬───────────────────┘         │ *add1: Add  │
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
		                                  │ terminator: *     │
		                                  └───────────────────┘
		""")
	public static Optional<MatchBaseIndexDisplacement1.Match> matchBaseIndexDisplacement1(Node node) {
		return MatchBaseIndexDisplacement1.match(node);
	}

	public static AddressingScheme handleMatchBaseIndexDisplacement1(MatchBaseIndexDisplacement1.Match match) {
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
		                            │ terminator: *     │
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
	 * @param match The match to check.
	 *
	 * @return {@code true} if this match is free from external dependants or {@code false} if it isn't.
	 */
	private static boolean isFreeFromExternalDependencies(BaseMatch match) {
		for (var n : match.markedNodes()) {

			// back edges for other nodes are only allowed inside match
			for (BackEdges.Edge edge : BackEdges.getOuts(n)) {
				var depender = edge.node;
				if (!match.matchedNodes().contains(depender)) {
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

	private static Graph getGraphFromMatch(BaseMatch match) {
		// cheesy way of getting graph
		return match.matchedNodes()
			.stream()
			.findFirst()
			.orElseThrow(() -> new IllegalStateException(
				"tried to retrieve graph from match but match did not even contain any node"))
			.getGraph();
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

				/* fiascii always anchors it's matches on the root node (node with no incoming edges in pattern)
				 * for all patterns in this class, this is the terminating op which must either be a store or load
				 *
				 * we didn't enforce this in the pattern, so we don't have to generate duplicated patterns, which
				 * would result in a lot of wasted time
				 *
				 * so do the check here in code
				 */
				if (!(node instanceof Load) && !(node instanceof Store)) {
					if (LOGGER.isDebugEnabled()) {
						var graph = getGraphFromMatch(match);
						LOGGER.debug("match [%s] in graph %s did not end in load/store op: %s", name, graph, match);
					}
					return Optional.empty();
				}

				var scheme = handler.apply(match);

				// check if marked node has already been marked by previous match
				for (var marked : match.markedNodes()) {
					if (replaced.contains(marked)) {
						return Optional.empty();
					}
				}

				// only marked nodes will be folded into addressing scheme, so we only need to check them
				if (!isFreeFromExternalDependencies(match)) {

					if (LOGGER.isDebugEnabled()) {
						var graph = getGraphFromMatch(match);
						LOGGER.debug("reject match [%s] in graph %s due to external dependencies: %s", name, graph,
							match);
					}

					// match failed, abort
					return Optional.empty();
				}

				// filter out match that can't be represented in x86
				if (isValidAddressingScheme(scheme)) {
					if (LOGGER.isDebugEnabled()) {
						var graph = getGraphFromMatch(match);
						LOGGER.debug("found match [%s] in graph %s: %s", name, graph, match);
					}
					return Optional.of(new Pair<>(scheme, match));
				}
				return Optional.empty();
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
