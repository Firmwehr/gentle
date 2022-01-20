package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irgopt;
import firm.nodes.Address;
import firm.nodes.Anchor;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Start;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TailCallOptimization extends NodeVisitor.Default {
	private static final Logger LOGGER = new Logger(TailCallOptimization.class, Logger.LogLevel.DEBUG);

	public static GraphOptimizationStep<Graph, Boolean> tailCallOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("TailCallOptimization")
			.withOptimizationFunction((graph) -> {
				// Needed to get all nodes belonging to a block
				BackEdges.enable(graph);
				List<TailCall> tailCalls = findTailCalls(graph);

				if (tailCalls.isEmpty()) {
					BackEdges.disable(graph);
					return false;
				}

				LOGGER.debugHeader("Creating jumps to replace those tail calls...");

				List<Node> loopJumps = new ArrayList<>();

				Node jumpFromStart = graph.newJmp(graph.getStartBlock());
				loopJumps.add(jumpFromStart);

				for (TailCall tailCall : tailCalls) {
					Node jump = graph.newJmp(tailCall.ret().getBlock());
					loopJumps.add(jump);
				}

				Node loopHeader = graph.newBlock(loopJumps.toArray(Node[]::new));
				graph.keepAlive(loopHeader);

				/* If the function does not branch, there are now two Jmps in the start block. We move the second one
				to the loop header. */
				for (int i = 1; i < loopJumps.size(); i++) {
					if (loopJumps.get(i).getBlock().equals(graph.getStartBlock())) {
						loopJumps.get(i).setBlock(loopHeader);
					}
				}

				// TODO: Encapsulate in some method
				Start start = graph.getStart();
				Proj startMem = null;
				List<Proj> startArgs = new ArrayList<>();

				for (BackEdges.Edge edge : BackEdges.getOuts(start)) {
					LOGGER.debug("%s", edge.node);
					// Check for Anchor
					if (edge.node instanceof Proj proj) {
						switch (proj.getNum()) {
							case 0:
								startMem = (Proj) edge.node;
								break;
							case 2:
								Util.outsStream(proj).forEach(node -> {
									// Check for Anchor, ignore instance used for call
									if (node instanceof Proj proj1 && !proj1.equals(proj)) {
										startArgs.add(proj1);
									}
								});
								break;
						}
					} else {
						LOGGER.debug("Ignoring Start out node %s", edge.node);
					}
				}

				LOGGER.debugHeader("Moving code that depends on start node from start block to loop header...");
				Set<Node> nodesToMove = new HashSet<>(successorsInBlock(startMem, graph.getStartBlock()));
				for (Proj arg : startArgs) {
					nodesToMove.addAll(successorsInBlock(arg, graph.getStartBlock()));
				}

				for (Node nodeToMove : nodesToMove) {
					LOGGER.debug("Moving %s to %s", nodeToMove, loopHeader);
					nodeToMove.setBlock(loopHeader);
				}

				LOGGER.debugHeader("Creating phis for arguments and start mem...");

				assert startMem != null;

				Phi memPhi =
					generatePhi(loopHeader, startMem, tailCalls.stream().map(tc -> tc.call().getPred(0)).toList());
				for (TailCall tc : tailCalls) {
					graph.keepAlive(tc.call().getPred(0));
				}

				Map<Proj, Phi> argPhis = new HashMap<>();
				for (Proj arg : startArgs) {
					LOGGER.debug("Generating Phi for argument %s %s", arg, arg.getNum());
					argPhis.put(arg, generatePhi(loopHeader, arg,
						tailCalls.stream().map(tc -> tc.call().getPred(arg.getNum() + 2)).toList()));
				}

				LOGGER.debugHeader("Deleting return nodes...");
				for (TailCall tc : tailCalls) {
					Graph.exchange(tc.ret(), graph.newBad(tc.ret().getMode()));
				}

				binding_irgopt.remove_bads(graph.ptr);

				BackEdges.disable(graph);
				return false;
			})
			.build();
	}

	// TODO: Fix awful implementation full of allocations
	private static Set<Node> successorsInBlock(Node arg, Block block) {
		Set<Node> successorsInBlock = new HashSet<>();
		Util.outsStream(arg).forEach((node) -> {
			LOGGER.debug("Investigating %s in block %s...", node, node.getBlock());
			if (node.getBlock() != null && node.getBlock().equals(block)) {
				successorsInBlock.add(node);
				successorsInBlock.addAll(successorsInBlock(node, block));
			}
		});
		return successorsInBlock;
	}

	// Right now this generates redundant phis for arguments that are not modified in the recursive call.
	// Let's hope someone cleans up after us :^)
	private static Phi generatePhi(Node node, Proj argLike, List<Node> otherInputs) {
		List<Node> ins = new ArrayList<>();
		ins.add(argLike);
		ins.addAll(otherInputs);
		Phi phi = (Phi) node.getGraph().newPhi(node, ins.toArray(Node[]::new), argLike.getMode());
		for (BackEdges.Edge user : BackEdges.getOuts(argLike)) {
			if (!(user.node instanceof Anchor) && !user.node.equals(phi)) {
				user.node.setPred(user.pos, phi);
			}
		}
		LOGGER.debug("Created new phi: %s", phi);
		return phi;
	}

	// TODO: Check edge cases, e.g. returning a local variable assigned in a block
	private static List<TailCall> findTailCalls(Graph graph) {
		LOGGER.debugHeader("Looking for tails calls in %d returns...", graph.getEndBlock().getPredCount());
		List<TailCall> tailCalls = new ArrayList<>();
		Block end = graph.getEndBlock();
		examineReturns:
		for (Node endPred : end.getPreds()) {
			LOGGER.debug("Examining node %s", endPred);
			assert endPred instanceof Return;
			Return ret = (Return) endPred;

			/*
			tail calls looks something like this:
						+---------+
						| Call    | <-- call
						+---------+
			              ^     ^
			              | 0   `--------\
			              |              | 0
			              |              |
			            +----------+   +-----------------+
			memProj --> | Proj M M |   | Proj T T_result | <-- tupleProj_i
			            +----------+   +-----------------+
			              ^              ^                                        ...
			              |              |
			              |              | 0
			              |            +----------+
			              | 0          | Proj _ 0 | <-- resultProj_i
			              |            +----------+
			              |              |
			              |  /-----------´
			              |  | 1
			              |  |
						+--------+
						| Return | <-- ret
						+--------+
			The underscore _ in the Proj input 1 of the Return is the return type of the Call (Bu, Is or P).
			For void calls the Return node only has the memory input.
			We need to check these properties for each Return ret:
			1) input 0 of ret is a Proj M memProj whose input 0 is a Call call
			2) call actually calls graph and not some other function
			3) for i in [1..ret.getPredCount()]: input i of ret is a Proj whose input 0 is a Proj T whose input 0 is
			call
			In gentle we could assume that ret.getPredCount() is either 1 (void) or 2 (boolean/int). In my opinion,
			the general solution is nicer though.
			 */

			// 1) Check direct mem dependency, find call
			if (!(ret.getPred(0) instanceof Proj memProj)) {
				continue;
			}
			assert memProj.getMode().equals(Mode.getM());
			if (!(memProj.getPred(0) instanceof Call call)) {
				continue;
			}

			// 2) Ensure that this is a recursive call
			Address calledFunction = (Address) call.getPred(1);
			if (!calledFunction.getEntity().equals(graph.getEntity())) {
				continue;
			}

			// 3) Check whether only results of call are returned
			for (int i = 1; i < ret.getPredCount(); i++) {
				if (!(ret.getPred(i) instanceof Proj resultProj)) {
					continue examineReturns;
				}
				if (!(resultProj.getPred(0) instanceof Proj tupleProj)) {
					continue examineReturns;
				}
				assert tupleProj.getMode().equals(Mode.getT());
				if (!tupleProj.getPred(0).equals(call)) {
					continue examineReturns;
				}

				LOGGER.debug("[%d] resultProj: %s tupleProj: %s", i, resultProj, tupleProj);
			}

			LOGGER.debug("%s is in a fact a tail call", endPred);
			tailCalls.add(new TailCall(ret, call));
		}

		return tailCalls;
	}

	private record TailCall(
		Return ret,
		Call call
	) {
	}
}