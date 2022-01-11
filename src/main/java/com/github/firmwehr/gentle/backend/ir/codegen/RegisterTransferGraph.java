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

@SuppressWarnings("UnstableApiUsage")
public class RegisterTransferGraph {

	private static final Logger LOGGER = new Logger(RegisterTransferGraph.class, Logger.LogLevel.DEBUG);

	private final MutableGraph<IkeaBøx> graph;
	private final Queue<IkeaBøx> freeRegisters;

	public RegisterTransferGraph(Set<IkeaBøx> freeRegisters) {
		this.freeRegisters = new ArrayDeque<>(freeRegisters);
		this.graph = GraphBuilder.directed().allowsSelfLoops(true).build();
	}

	public void addMove(IkeaBøx source, IkeaBøx target) {
		graph.putEdge(source, target);
		LOGGER.debug("Added move %s -> %s", source, target);
	}

	public List<Pair<IkeaBøx, IkeaBøx>> generateMoveSequence() {
		if (graph.edges().isEmpty()) {
			LOGGER.debug("Nothing to move!");
			return List.of();
		}

		List<Pair<IkeaBøx, IkeaBøx>> generatedMoves = new ArrayList<>();
		generateEasyMoves(generatedMoves);

		// Unbelievable
		if (graph.edges().isEmpty()) {
			LOGGER.debug("Easy was enough!");
			return generatedMoves;
		}
		LOGGER.debug("Doing it the hard way");
		// Pain. We have cycles (not the renderer).

		// Moves:
		// temp <- r1
		// r2   <- r1
		// r3   <- r2
		// r4   <- r3
		// ...
		// rn   <- temp
		if (!freeRegisters.isEmpty()) {
			IkeaBøx tempRegister = freeRegisters.poll();
			LOGGER.debug("Free register found, using %s as a scratchpad", tempRegister);

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

				// r2   <- r1
				// r3   <- r2
				// r4   <- r3
				IkeaBøx source = startEdge.source();
				while (!graph.predecessors(source).isEmpty()) {
					LOGGER.debug("Entered with source %s and preds %s", source, graph.predecessors(source));
					IkeaBøx target = graph.predecessors(source).iterator().next();
					generatedMoves.add(new Pair<>(target, source));
					LOGGER.debug("Adding move %s -> %s", target, source);
					graph.removeEdge(target, source);
					source = target;
				}

				// rn   <- temp
				generatedMoves.add(new Pair<>(tempRegister, source));
				LOGGER.debug("Adding move %s -> %s", tempRegister, source);
				graph.removeEdge(source, startEdge.source());
			}
		} else {
			throw new InternalCompilerException("Here be transpositions, only relevant if registers are limited");
		}

		return generatedMoves;
	}

	private void generateEasyMoves(List<Pair<IkeaBøx, IkeaBøx>> generatedMoves) {
		Optional<EndpointPair<IkeaBøx>> edgeOpt;
		while ((edgeOpt = findFreeTarget()).isPresent()) {
			EndpointPair<IkeaBøx> edge = edgeOpt.get();
			LOGGER.debug("Found victim %s -> %s", edge.source(), edge.target());
			generatedMoves.add(new Pair<>(edge.source(), edge.target()));

			// Remove handled edge
			graph.removeEdge(edge);

			//			// Free as early as possible: Rewire to read from target
			//			for (IkeaBøx successor : graph.successors(edge.source())) {
			//				// TODO: Why can we leave self loops in peace and not redirect them, if we mark source as
			//				 free?
			//				// We might want to do that here!
			//				graph.removeEdge(edge.source(), successor);
			//				graph.putEdge(edge.target(), successor);
			//			}
			//
			//			freeRegisters.add(edge.source());
		}
	}

	private Optional<EndpointPair<IkeaBøx>> findFreeTarget() {
		for (EndpointPair<IkeaBøx> edge : graph.edges()) {
			if (graph.outDegree(edge.target()) == 0) {
				return Optional.of(edge);
			}
		}
		return Optional.empty();
	}
}
