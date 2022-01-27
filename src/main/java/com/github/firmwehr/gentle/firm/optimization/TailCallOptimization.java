package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.TailCallPattern;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irgopt;
import firm.nodes.Anchor;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Jmp;
import firm.nodes.Node;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Start;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TailCallOptimization {
	private static final Logger LOGGER = new Logger(TailCallOptimization.class, Logger.LogLevel.DEBUG);

	public static GraphOptimizationStep<Graph, Boolean> tailCallOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("TailCallOptimization")
			.withOptimizationFunction((graph) -> {
				// Needed to get all nodes belonging to a block
				BackEdges.enable(graph);

				/*
				Identify tail calls in this graph, e.g. f(i) { if i == 0 { return 1; } else { return f(i - 1); }.
				If there are none, we can't do anything.
				 */
				List<TailCall> tailCalls = findTailCalls(graph);
				if (tailCalls.isEmpty()) {
					BackEdges.disable(graph);
					return false;
				}

				LOGGER.debugHeader("Creating a jump to replace each tail call...");

				List<Node> loopJumps = new ArrayList<>();
				Node jumpFromStart = graph.newJmp(graph.getStartBlock());
				loopJumps.add(jumpFromStart);
				Optional<Jmp> selfJmp = Optional.empty();

				for (TailCall tailCall : tailCalls) {
					Jmp jump = (Jmp) graph.newJmp(tailCall.ret().getBlock());
					loopJumps.add(jump);
					/*
					The start block already contains a Jmp (jumpFromStart). However, since the loopHeader block
					doesn't exist yet we will create the node anyway and update the block later (see next comment).
					 */
					if (tailCall.ret().getBlock().equals(graph.getStartBlock())) {
						selfJmp = Optional.of(jump);
					}
				}

				Block loopHeader = (Block) graph.newBlock(loopJumps.toArray(Node[]::new));
				/*
				If the loop header needs to jump into itself, update the target block.
				 */
				selfJmp.ifPresent((jump) -> jump.setBlock(loopHeader));
				graph.keepAlive(loopHeader);

				GraphInputs inputs = getInputs(graph);

				/*
				Code in the start block that depends on the input memory or arguments must be moved into the loop
				header
				where we will create some phis to use as inputs instead.
				 */
				LOGGER.debugHeader("Moving code that depends on start node from start block to loop header...");
				Set<Node> nodesToMove = new HashSet<>(successorsInBlock(inputs.mem(), graph.getStartBlock()));
				for (Proj arg : inputs.arguments()) {
					nodesToMove.addAll(successorsInBlock(arg, graph.getStartBlock()));
				}
				for (Node nodeToMove : nodesToMove) {
					LOGGER.debug("Moving %s to %s", nodeToMove, loopHeader);
					nodeToMove.setBlock(loopHeader);
				}

				LOGGER.debugHeader("Creating phis for arguments and start mem...");
				generatePhi(loopHeader, inputs.mem(), tailCalls.stream().map(tc -> tc.call().getPred(0)).toList());
				for (TailCall tc : tailCalls) {
					graph.keepAlive(tc.call().getPred(0));
				}
				for (Proj arg : inputs.arguments()) {
					LOGGER.debug("Generating Phi for argument %s %s", arg, arg.getNum());
					generatePhi(loopHeader, arg,
						tailCalls.stream().map(tc -> tc.call().getPred(arg.getNum() + 2)).toList());
				}

				LOGGER.debugHeader("Deleting return nodes...");
				for (TailCall tc : tailCalls) {
					Graph.exchange(tc.ret(), graph.newBad(tc.ret().getMode()));
				}
				binding_irgopt.remove_bads(graph.ptr);

				BackEdges.disable(graph);
				return true;
			})
			.build();
	}

	/**
	 * Contains input memory, base pointer and arguments of a graph.
	 */
	private record GraphInputs(
		Proj mem,
		Proj bp,
		List<Proj> arguments
	) {
	}

	private static GraphInputs getInputs(Graph graph) {
		Start start = graph.getStart();
		Optional<Proj> mem = Optional.empty();
		Optional<Proj> bp = Optional.empty();
		List<Proj> arguments = new ArrayList<>();

		for (Node node : Util.outsStream(start).toList()) {
			// Check for Anchor
			if (node instanceof Proj proj) {
				switch (proj.getNum()) {
					case 0 -> mem = Optional.of((Proj) node);
					case 1 -> bp = Optional.of((Proj) node);
					case 2 -> Util.outsStream(proj).forEach(argNode -> {
						// Check for Anchor, ignore instance used for call
						if (argNode instanceof Proj argProj) {
							arguments.add(argProj);
						}
					});
				}
			} else {
				LOGGER.debug("Ignoring Start out node %s", node);
			}
		}

		assert mem.isPresent() && mem.get().getMode().equals(Mode.getM());
		assert bp.isPresent() && bp.get().getMode().equals(Mode.getP());

		return new GraphInputs(mem.get(), bp.get(), arguments);
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
	private static void generatePhi(Node node, Proj argLike, List<Node> otherInputs) {
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

			matchTailCall(ret).ifPresent((tc) -> {
				LOGGER.debug("%s is in a fact a tail call", endPred);
				tailCalls.add(tc);
			});
		}

		return tailCalls;
	}

	public static Optional<TailCall> matchTailCall(Node node) {
		return matchTailCall_(node).flatMap((match) -> {
			if (match.ret() instanceof Return ret && match.call() instanceof Call call) {
				return Optional.of(new TailCall(ret, call));
			} else {
				return Optional.empty();
			}
		});
	}

	@FiAscii("""
		            ┌───────┐                          While it is technically possible
		            │call: *├───────┐                  that a call returns multiple values,
		            └┬──────┘       │                  the gentle frontend does not generate
		             │              │                  such code. Therefore we only need to
		┌────────────▼──────────┐  ┌▼──────────────┐   consider tail calls with a single
		│memProj: Proj ; +memory│  │tupleProj: Proj│   return value.
		└────────────┬──────────┘  └┬──────────────┘
		             │              │
		             │  ┌───────────▼────┐
		             │  │resultProj: Proj│
		             │  └─┬──────────────┘
		             │    │
		            ┌▼────▼┐
		            │ret: *│
		            └──────┘""")
	public static Optional<TailCallPattern.Match> matchTailCall_(Node node) {
		return TailCallPattern.match(node);
	}

	private record TailCall(
		Return ret,
		Call call
	) {
	}
}