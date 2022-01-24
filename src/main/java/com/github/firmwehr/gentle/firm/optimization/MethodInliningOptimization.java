package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Pair;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irgraph;
import firm.bindings.binding_irnode;
import firm.bindings.binding_irop;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Call;
import firm.nodes.Const;
import firm.nodes.End;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Start;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class MethodInliningOptimization {
	private static final Logger LOGGER = new Logger(MethodInliningOptimization.class, Logger.LogLevel.DEBUG);
	// values carefully picked by having no clue
	private static final double CALLER_INLINE_COST_THRESHOLD = 256;
	private final CallGraph callGraph;

	private MethodInliningOptimization(CallGraph callGraph) {
		this.callGraph = callGraph;
	}

	public static GraphOptimizationStep<CallGraph, Set<Graph>> methodInlineOptimization() {
		return GraphOptimizationStep.<CallGraph, Set<Graph>>builder()
			.withDescription("MethodInlining")
			.withOptimizationFunction(callGraph -> new MethodInliningOptimization(callGraph).optimize())
			.build();
	}

	private Set<Graph> optimize() {
		Map<Graph, Double> inlineCandidates = new HashMap<>();
		Set<Graph> changed = new HashSet<>();
		callGraph.walkPostorder(graph -> {
			double selfCost = CostCalculator.calculate(graph).cost();
			if (selfCost <= CALLER_INLINE_COST_THRESHOLD) {
				List<InlineCandidate> localCandidates = findInlineCandidates(graph, inlineCandidates);
				if (inlineAny(graph, localCandidates)) {
					LOGGER.debug("inlined -> modified %s", graph.getEntity().getLdName());
					changed.add(graph);
					selfCost = CostCalculator.calculate(graph).cost();
					GraphDumper.dumpGraph(graph, "method-inlined");
					graph.confirmProperties(
						binding_irgraph.ir_graph_properties_t.IR_GRAPH_PROPERTIES_NONE);
				}
			} else {
				LOGGER.debug("skipping because of self cost");
			}
			inlineCandidates.put(graph, selfCost);
		});
		LOGGER.debug("modified %s", changed);
		return changed;
	}

	private boolean inlineAny(Graph graph, List<InlineCandidate> candidates) {
		double approxAddedCost = 0;
		// we want to inline cheap methods first (as calling overhead is dominant there)
		candidates.sort(Comparator.comparingDouble(InlineCandidate::cost));
		LOGGER.info("inline candidates for %s: %s", graph.getEntity().getLdName(), candidates);
		for (InlineCandidate c : candidates) {
			if (approxAddedCost + c.cost() > CALLER_INLINE_COST_THRESHOLD) {
				LOGGER.debug("no more inlining due to costs (%s)", c);
				break;
			}
			inline(graph, c);
			approxAddedCost += c.cost();
		}
		LOGGER.debug("inlined any = %s", approxAddedCost > 0);
		return approxAddedCost > 0; // true if anything was inlined
	}

	private void inline(Graph graph, InlineCandidate candidate) {
		Map<Node, Node> calleeToCallerCopied = new HashMap<>();
		Graph toInline = candidate.graph();
		Call call = candidate.call();
		Node oldBlock = splitBlockAt(call);
		Map<Mode, List<BackEdges.Edge>> users = findUsers(call);
		List<Return> returns = new ArrayList<>();
		List<Pair<Node, Integer>> delayed = new ArrayList<>();
		toInline.walkBlocksPostorder(old -> {
			if (old.getGraph().getStartBlock().equals(old)) {
				// call.getBlock() is newly created block
				calleeToCallerCopied.put(old, call.getBlock());
				return;
			}
			Node copyBlock = old.copyInto(graph);
			calleeToCallerCopied.put(old, copyBlock);
			for (int i = 0; i < old.getPredCount(); i++) {
				// set the new pred
				copyBlock.setPred(i, graph.newUnknown(old.getPred(i).getMode()));
			}
		});
		toInline.walkPostorder(new NodeVisitor.Default() {

			@Override
			public void visit(Proj node) {
				if (node.getPred().equals(toInline.getArgs())) {
					calleeToCallerCopied.put(node, call.getPred(2 + node.getNum()));
				} else if (node.getMode().equals(Mode.getM()) && node.getPred().equals(toInline.getStart())) {
					// memory Proj from start
					calleeToCallerCopied.put(node, call.getMem());
				} else if (!node.equals(toInline.getArgs())) {
					defaultVisit(node);
				}
			}

			@Override
			public void visit(Start node) {
				// ignore start, handled in Proj
			}

			@Override
			public void visit(Return node) {
				returns.add(node);
			}

			@Override
			public void visit(End node) {
				Node[] jumps = new Node[returns.size()];
				for (int i = 0; i < returns.size(); i++) {
					jumps[i] = graph.newJmp(calleeToCallerCopied.get(returns.get(i).getBlock()));
					calleeToCallerCopied.put(returns.get(i), jumps[i]);
				}

				binding_irnode.set_irn_in(oldBlock.ptr, jumps.length, Node.getBufferFromNodeList(jumps));

				Map<Mode, List<Node>> phis = new HashMap<>();
				for (Return ret : returns) {
					for (Node pred : ret.getPreds()) {
						phis.computeIfAbsent(pred.getMode(), ignored -> new ArrayList<>())
							.add(calleeToCallerCopied.get(pred));
					}
				}
				for (Map.Entry<Mode, List<BackEdges.Edge>> entry : users.entrySet()) {
					List<Node> ins = phis.get(entry.getKey());
					Node phi = graph.newPhi(oldBlock, ins.toArray(Node[]::new), entry.getKey());
					for (BackEdges.Edge userEdge : entry.getValue()) {
						userEdge.node.setPred(userEdge.pos, phi);
					}
				}
			}

			@Override
			public void visit(Block old) {

			}

			@Override
			public void defaultVisit(Node old) {
				Node copy = old.copyInto(graph);
				if (belongsToStart(copy)) {
					copy.setBlock(graph.getStartBlock());
				} else {
					copy.setBlock(calleeToCallerCopied.get(old.getBlock()));
				}
				calleeToCallerCopied.put(old, copy);
				for (int i = 0; i < old.getPredCount(); i++) {
					// set the new pred
					Node newNode = calleeToCallerCopied.get(old.getPred(i));
					if (newNode == null) {
						delayed.add(new Pair<>(old, i));
						newNode = graph.newUnknown(old.getPred(i).getMode());
					}
					copy.setPred(i, newNode);
				}
			}
		});
		for (Pair<Node, Integer> pair : delayed) {
			Node newNode = calleeToCallerCopied.get(pair.first());
			Node newPred = calleeToCallerCopied.get(pair.first().getPred(pair.second()));
			newNode.setPred(pair.second(), newPred);
		}
		toInline.walkBlocksPostorder(old -> {
			Node newBlock = calleeToCallerCopied.get(old);
			for (int i = 0; i < old.getPredCount(); i++) {
				newBlock.setPred(i, calleeToCallerCopied.get(old.getPred(i)));
			}
		});
		LOGGER.info("inlined %s into %s", toInline, graph);
	}

	private List<InlineCandidate> findInlineCandidates(Graph graph, Map<Graph, Double> candidates) {
		GentleBindings.ir_estimate_execfreq(graph.ptr);
		BackEdges.disable(graph);
		List<InlineCandidate> result = new ArrayList<>();
		Set<Call> calls = callGraph.callSitesIn(graph);
		for (Call call : calls) {
			Graph called = ((Address) call.getPtr()).getEntity().getGraph();
			if (called != null) { // is null for runtime calls
				double cost = candidates.getOrDefault(called, Double.MAX_VALUE);
				if (cost >= Double.MAX_VALUE) {
					LOGGER.debug("no cost available for %s", called.getEntity().getLdName());
					continue; // skip this call, cannot be inlined
				}
				cost -= benefit(call, cost / 2);
				result.add(new InlineCandidate(call, called, cost));
			}
		}
		return result;
	}

	private double benefit(Call call, double halvedCost) {
		double benefit = 0;
		int paramCount = call.getPredCount() - 1; // -2 + 1, to avoid div by 0
		for (Node pred : call.getPreds()) {
			boolean comesFromAllocCall = pred instanceof Proj proj && proj.getMode().equals(Mode.getP()) &&
				proj.getPred() instanceof Call allocCall && Util.isAllocCall(allocCall);
			if (pred instanceof Const) {
				benefit += halvedCost / paramCount;
			} else if (comesFromAllocCall) {
				benefit += halvedCost / (paramCount + 2); // might be an escape analysis candidate if inlined
			} else {
				benefit += halvedCost / (paramCount * 4);
			}
		}
		return benefit * GentleBindings.get_block_execfreq(call.ptr);
	}

	record InlineCandidate(
		Call call,
		Graph graph,
		double cost
	) {

	}

	private Map<Mode, List<BackEdges.Edge>> findUsers(Call call) {
		BackEdges.enable(call.getGraph());
		Map<Mode, List<BackEdges.Edge>> users = new HashMap<>();
		Queue<Node> nodesToSkip = new ArrayDeque<>();
		nodesToSkip.add(call);
		while (!nodesToSkip.isEmpty()) {
			Node next = nodesToSkip.remove();
			for (BackEdges.Edge edge : BackEdges.getOuts(next)) {
				if (edge.node instanceof Proj) {
					nodesToSkip.add(edge.node);
				} else {
					users.computeIfAbsent(next.getMode(), ignored -> new ArrayList<>()).add(edge);
				}
			}
		}
		BackEdges.disable(call.getGraph());
		return users;
	}

	private Node splitBlockAt(Call call) {
		Graph graph = call.getGraph();
		Node oldBlock = call.getBlock();
		Node newBlock = graph.newBlock(predArray(oldBlock));
		BackEdges.enable(graph);
		if (graph.getStartBlock().equals(oldBlock)) {
			graph.getArgs().setBlock(newBlock);
			for (BackEdges.Edge edge : BackEdges.getOuts(graph.getArgs())) {
				edge.node.setBlock(newBlock);
			}
			graph.getNoMem().setBlock(newBlock);
			graph.getInitialMem().setBlock(newBlock);
			graph.getFrame().setBlock(newBlock);
			graph.setStartBlock((Block) newBlock);
			for (BackEdges.Edge edge : BackEdges.getOuts(oldBlock)) {
				if (belongsToStart(edge.node)) {
					edge.node.setBlock(graph.getStartBlock());
				}
			}
		} else {
			for (BackEdges.Edge edge : BackEdges.getOuts(oldBlock)) {
				if (edge.node instanceof Phi) {
					edge.node.setBlock(newBlock);
				}
			}
		}
		movePredecessors(call, oldBlock, newBlock);
		BackEdges.disable(graph);
		return oldBlock;
	}

	private void movePredecessors(Node node, Node oldBlock, Node newBlock) {
		if (belongsToStart(node)) {
			node.setBlock(oldBlock.getGraph().getStartBlock());
		} else {
			node.setBlock(newBlock);
		}
		moveProjEdges(node, newBlock);
		if (node instanceof Phi) {
			return;
		}
		for (Node pred : node.getPreds()) {
			if (pred.getBlock().equals(oldBlock)) {
				movePredecessors(pred, oldBlock, newBlock);
			}
		}
	}

	private void moveProjEdges(Node node, Node newBlock) {
		node.setBlock(newBlock);
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			if (edge.node instanceof Proj) {
				moveProjEdges(edge.node, newBlock);
			}
		}
	}

	private boolean belongsToStart(Node node) {
		Pointer op = binding_irnode.get_irn_op(node.ptr);
		int flags = binding_irop.get_op_flags(op);
		return (flags & binding_irop.irop_flags.irop_flag_start_block.val) != 0;
	}

	private Node[] predArray(Node node) {
		Node[] nodes = new Node[node.getPredCount()];
		for (int i = 0; i < node.getPredCount(); i++) {
			nodes[i] = node.getPred(i);
		}
		return nodes;
	}

	// TODO better loop detection (review freq)??
	static class CostCalculator extends NodeVisitor.Default {
		private static final double PHI_COST = 3d;
		private static final double CONDITIONAL_JUMP_COST = 2d; // Proj X false OR Proj X true => 4 per Jmp
		private static final double RUNTIME_CALL_COST = 1.5;
		private static final double METHOD_CALL_COST = 2.5;
		private final Graph graph;

		private double cost;

		public CostCalculator(Graph graph) {
			this.graph = graph;
		}

		static CostCalculator calculate(Graph graph) {
			CostCalculator calculator = new CostCalculator(graph);
			// if the End block does not only have Return preds, we rather not inline that method
			// as it might be an infinite loop => not worth
			for (Node pred : graph.getEnd().getPreds()) {
				if (!(pred instanceof Return)) {
					calculator.cost = Double.POSITIVE_INFINITY;
					return calculator;
				}
			}
			graph.walk(calculator);
			return calculator;
		}

		public double cost() {
			return cost;
		}

		@Override
		public void defaultVisit(Node n) {
			for (Node pred : n.getPreds()) {
				count(pred);
			}
		}

		private void count(Node node) {
			cost += GentleBindings.get_block_execfreq(node.ptr) * switch (node) {
				case Call call -> {
					if (((Address) call.getPtr()).getEntity().getGraph() == null) {
						yield RUNTIME_CALL_COST;
					}
					if (((Address) call.getPtr()).getEntity().equals(graph.getEntity())) {
						// we really want to prevent inlining of recursive calls
						yield Double.POSITIVE_INFINITY;
					}
					yield METHOD_CALL_COST;
				}
				case Phi ignored -> PHI_COST;
				case Proj proj -> {
					if (proj.getMode().equals(Mode.getX())) {
						yield CONDITIONAL_JUMP_COST;
					}
					yield 0d;
				}
				case Return ignored -> CONDITIONAL_JUMP_COST;
				default -> 1d;
			};
		}
	}
}
