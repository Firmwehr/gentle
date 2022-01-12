package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.backend.ir.IkeaBøx;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.Pair;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Builds and solves register transfer graphs by decomposing the register permutation into an ordered sequence of
 * register-register moves.
 * <p>
 * This is not trivial as the graph induced by the permutation might have cycles (e.g. {@code A -> B, B -> A} if two
 * registers are swapped). The cycles can be of arbitrary length, if registers are just shifted by one, e.g. {@code A ->
 * B -> C -> D}...
 * <p>
 * Cycles are broken by using an additional temporary register if one is available. If no free register is available, an
 * ordered sequence of register swaps is generated.
 */
@SuppressWarnings("UnstableApiUsage")
public class RegisterTransferGraph {

	private static final Logger LOGGER = new Logger(RegisterTransferGraph.class);

	private final MutableGraph<IkeaBøx> graph;
	private final Queue<IkeaBøx> freeRegisters;

	public RegisterTransferGraph(Set<IkeaBøx> freeRegisters) {
		this.freeRegisters = new ArrayDeque<>(freeRegisters);
		this.graph = GraphBuilder.directed().allowsSelfLoops(true).build();
	}

	/**
	 * Adds a move edge to this graph. This ensures that after this transfer the value in {@code target} must match the
	 * value stored in {@code source} <em>before the transfer</em>.
	 *
	 * @param source the source of the edge
	 * @param target the target of the edge
	 */
	public void addMove(IkeaBøx source, IkeaBøx target) {
		graph.putEdge(source, target);
		LOGGER.debug("Added move %s -> %s", source, target);
	}

	/**
	 * Generates a valid solution for this register transfer graph.
	 *
	 * @return an ordered sequence of moves from {@link Pair#first()} tp {@link Pair#second()}
	 */
	public List<Pair<IkeaBøx, IkeaBøx>> generateMoveSequence() {
		if (graph.edges().isEmpty()) {
			LOGGER.debug("Nothing to move!");
			return List.of();
		}

		List<Pair<IkeaBøx, IkeaBøx>> generatedMoves = new ArrayList<>();
		solveStraightPaths(generatedMoves);

		// Unbelievable
		if (graph.edges().isEmpty()) {
			LOGGER.debug("Easy was enough!");
			return generatedMoves;
		}
		LOGGER.debug("Doing it the hard way");
		// Pain. We have cycles (not the renderer).

		if (!freeRegisters.isEmpty()) {
			IkeaBøx tempRegister = freeRegisters.iterator().next();
			LOGGER.debug("Free register found, using %s as a scratchpad", tempRegister);
			solveCyclesWithFreeRegister(generatedMoves, tempRegister);
		} else {
			throw new InternalCompilerException("Here be transpositions, only relevant if registers are limited");
		}

		if (!graph.edges().isEmpty()) {
			throw new InternalCompilerException(
				"Register transfer graph could not be solved. Leftover edges: " + graph.edges());
		}

		return generatedMoves;
	}

	private void solveCyclesWithFreeRegister(List<Pair<IkeaBøx, IkeaBøx>> generatedMoves, IkeaBøx tempRegister) {
		// Moves:
		// temp <- r1
		// r1   <- r2
		// r2   <- r3
		// r3   <- r4
		// ...
		// rn   <- temp

		while (!graph.edges().isEmpty()) {
			EndpointPair<IkeaBøx> startEdge = graph.edges().iterator().next();
			LOGGER.debug("Processing %s -> %s", startEdge.source(), startEdge.target());

			if (startEdge.source().equals(startEdge.target())) {
				LOGGER.debug("Self-loop found, NOPing out");
				graph.removeEdge(startEdge);
				continue;
			}

			// temp <- r1
			generatedMoves.add(new Pair<>(startEdge.source(), tempRegister));
			LOGGER.debug("Adding move %s -> %s", startEdge.source(), tempRegister);
			IkeaBøx lastNode = startEdge.target();

			// r1   <- r2
			// r2   <- r3
			// r3   <- r4
			IkeaBøx current = startEdge.source();
			while (!graph.predecessors(current).isEmpty()) {
				LOGGER.debug("Entered with current %s and preds %s", current, graph.predecessors(current));
				IkeaBøx source = graph.predecessors(current).iterator().next();
				generatedMoves.add(new Pair<>(source, current));
				LOGGER.debug("Adding move %s -> %s", source, current);
				graph.removeEdge(source, current);
				current = source;
			}

			// rn   <- temp
			generatedMoves.add(new Pair<>(tempRegister, lastNode));
			LOGGER.debug("Adding move %s -> %s", tempRegister, lastNode);
			graph.removeEdge(startEdge.source(), lastNode);
		}
	}

	/**
	 * Finds all "straight paths", i.e. sequences of moves like {@code A -> B -> C ... -> Z} that do <em>not</em>
	 * form a
	 * cycle. Such paths can be solved by moving registers in reverse order: the last one can be safely overwritten as
	 * its value is never used.
	 *
	 * @param generatedMoves the list to store generated moves in
	 */
	private void solveStraightPaths(List<Pair<IkeaBøx, IkeaBøx>> generatedMoves) {
		Optional<EndpointPair<IkeaBøx>> edgeOpt;
		//noinspection NestedAssignment
		while ((edgeOpt = findEdgeWithUnusedTarget()).isPresent()) {
			EndpointPair<IkeaBøx> edge = edgeOpt.get();
			LOGGER.debug("Found victim %s -> %s", edge.source(), edge.target());
			generatedMoves.add(new Pair<>(edge.source(), edge.target()));

			// Remove handled edge
			graph.removeEdge(edge);

			// Free as early as possible: Rewire to read from target
			for (IkeaBøx successor : graph.successors(edge.source())) {
				// TODO: Verify this code is correct once we can have self loops with pyhsical registers
				// Do not rewire self loops to save a move
				if (edge.source().equals(successor)) {
					LOGGER.debug("Skipped self loop: %s -> %s", edge.source(), successor);
					continue;
				}
				LOGGER.debug("Rewriting out edge to %s to start at %s", successor, edge.target());
				graph.removeEdge(edge.source(), successor);
				graph.putEdge(edge.target(), successor);
			}

			// It is only free if we do not have a self loop
			if (!graph.successors(edge.source()).contains(edge.source())) {
				// TODO: Verify this code is correct once we can have self loops with pyhsical registers
				LOGGER.debug("Freeing register %s as it has no self loop", edge.source());
				freeRegisters.add(edge.source());
			} else {
				LOGGER.debug("Keeping register %s as it has a self loop", edge.source());
			}
		}
	}

	/**
	 * @return an edge where the target has outdegree 0, if one exists
	 */
	private Optional<EndpointPair<IkeaBøx>> findEdgeWithUnusedTarget() {
		for (EndpointPair<IkeaBøx> edge : graph.edges()) {
			if (graph.outDegree(edge.target()) == 0) {
				return Optional.of(edge);
			}
		}
		return Optional.empty();
	}
}
