package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.output.Logger;
import firm.Graph;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Return;

import java.util.ArrayList;
import java.util.List;

public class TailCallOptimization extends NodeVisitor.Default {
	private static final Logger LOGGER = new Logger(TailCallOptimization.class, Logger.LogLevel.DEBUG);

	public static GraphOptimizationStep<Graph, Boolean> tailCallOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("Tail Call Optimization")
			.withOptimizationFunction((graph) -> {
				List<Return> tailCalls = findTailCalls(graph);

				LOGGER.debugHeader("Replacing %d tail calls by jumps...", tailCalls.size());

				return false;
			})
			.build();
	}

	private static List<Return> findTailCalls(Graph graph) {
		LOGGER.debugHeader("Looking for tails calls in %d returns...", graph.getEndBlock().getPredCount());
		List<Return> tailCalls = new ArrayList<>();
		for (Node endPred : graph.getEndBlock().getPreds()) {
			LOGGER.debug("Examining node %s", endPred);
			assert endPred instanceof Return;
			Return ret = (Return) endPred;

			// First ret pred is a memory proj, skip that
			// Check whether last mem before return came from a call
			if (!(ret.getPred(0).getPred(0) instanceof Call)) {
				continue;
			}

			Call retCall = (Call) ret.getPred(0).getPred(0);
			Address retCallFun = (Address) retCall.getPred(1);

			if (retCallFun.getEntity().equals(graph.getEntity())) {
				LOGGER.debug("%s is in a fact a tail call", endPred);
				tailCalls.add(ret);
			}
		}
		return tailCalls;
	}
}