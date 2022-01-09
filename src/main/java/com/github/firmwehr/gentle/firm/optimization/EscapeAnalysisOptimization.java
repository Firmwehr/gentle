package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.construction.StdLibEntity;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Load;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Store;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EscapeAnalysisOptimization {
	private static final Logger LOGGER = new Logger(EscapeAnalysisOptimization.class, Logger.LogLevel.DEBUG);

	private final Graph graph;

	public EscapeAnalysisOptimization(Graph graph) { // TODO private
		this.graph = graph;
	}

	public boolean optimize() {
		LOGGER.debug("%s", graph);
		Set<Call> allocationCalls = new HashSet<>();
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				if (((Address) node.getPtr()).getEntity().equals(StdLibEntity.ALLOCATE.getEntity())) {
					filterNonEscapingAllocations(node).ifPresent(allocationCalls::add);
				}
			}
		});
		rewriteAll(allocationCalls);
		BackEdges.disable(graph);
		return true; // TODO
	}

	private void rewriteAll(Set<Call> allocationCalls) {
		for (Call call : allocationCalls) {
			rewrite(call);
		}
	}

	private void rewrite(Call call) {
		// TODO
		for (BackEdges.Edge edge : BackEdges.getOuts(call)) {
			if (edge.node instanceof Proj proj && proj.getMode().equals(Mode.getT())) {
				for (BackEdges.Edge argEdge : BackEdges.getOuts(edge.node)) {

				}
			}
		}
	}

	private Optional<Call> filterNonEscapingAllocations(Call allocationCall) {
		if (!escapes(allocationCall)) {
			return Optional.of(allocationCall);
		}
		return Optional.empty();
	}

	private boolean escapes(Call call) {
		Deque<Node> deepOuts = new ArrayDeque<>();
		deepOuts.add(call);
		while (!deepOuts.isEmpty()) {
			Node next = deepOuts.removeLast();
			for (BackEdges.Edge edge : BackEdges.getOuts(next)) {
				if (edge.node instanceof Call || edge.node instanceof Return) {
					LOGGER.debug("%s escapes on %s", call, edge.node);
					return true;
				}
				if (edge.node instanceof Load) {
					// Load from this call => ignore (would be replaced)
					LOGGER.debug("stop at %s", edge.node);
					continue;
				}
				if (edge.node instanceof Store store) {
					if (storeIn(store.getPtr(), call)) {
						// Store *in* allocated object would be replaced
						LOGGER.debug("stop at %s", edge.node);
						continue;
					}
					// store in other object, mark this as escape. If other object does not escape, this
					// can be optimized when running escape analysis again
					return true;
				}
				if (edge.node instanceof Cmp) {
					// a == b => boolean, no need to go further
					LOGGER.debug("stop at %s", edge.node);
					continue;
				}
				if (edge.node instanceof Proj proj && proj.getMode().equals(Mode.getM())) {
					// don't follow memory nodes
					LOGGER.debug("stop at %s", edge.node);
					continue;
				}
				// a node that can't be ignored
				deepOuts.add(edge.node);
			}
		}
		LOGGER.debug("%s does not escape, can be optimized", call);
		return false;
	}

	private boolean storeIn(Node storePred, Call call) {
		if (storePred == call) {
			return true;
		}
		for (Node pred : storePred.getPreds()) {
			if (storeIn(pred, call)) {
				return true;
			}
		}
		return false;
	}
}
