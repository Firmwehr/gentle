package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.firm.optimization.callgraph.CallGraph;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irnode;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Call;
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
	private static final double CALLEE_INLINE_COST_THRESHOLD = 16d;
	private static final double CALLER_INLINE_COST_THRESHOLD = 32d;
	private static final double INCREASE_THRESHOLD = 32d;
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
			double selfCost = new CostCalculator(graph).cost();
			if (selfCost > CALLER_INLINE_COST_THRESHOLD) {
				return;
			}
			List<InlineCandidate> localCandidates = findInlineCandidates(graph, inlineCandidates);
			if (inlineAny(graph, localCandidates)) {
				changed.add(graph);
				selfCost = new CostCalculator(graph).cost();
				GraphDumper.dumpGraph(graph, "method-inlined");
			}
			inlineCandidates.put(graph, selfCost);
		});
		return changed;
	}

	private boolean inlineAny(Graph graph, List<InlineCandidate> candidates) {
		double approxAddedCost = 0;
		// we want to inline cheap methods first (as calling overhead is dominant there)
		candidates.sort(Comparator.comparingDouble(InlineCandidate::cost));
		LOGGER.debug("candidates %s", candidates);
		for (InlineCandidate c : candidates) {
			inline(graph, c);
			approxAddedCost += c.cost();
			if (approxAddedCost > INCREASE_THRESHOLD) {
				return true;
			}
		}
		return approxAddedCost > 0; // true if anything was inlined
	}

	private void inline(Graph graph, InlineCandidate candidate) {
		Map<Node, Node> calleeToCallerCopied = new HashMap<>();
		Graph toInline = candidate.graph();
		Call call = candidate.call();
		Node oldBlock = splitBlockAt(call);
		Map<Mode, List<BackEdges.Edge>> users = findUsers(call);
		List<Return> returns = new ArrayList<>();
		toInline.walkPostorder(new NodeVisitor.Default() {

			@Override
			public void visit(Proj node) {
				if (node.getPred().equals(toInline.getArgs())) {
					LOGGER.debug("insert argument %s %s with pred %s", node, node.getNum(), call.getPred(2 + node.getNum()));
					calleeToCallerCopied.put(node, call.getPred(2 + node.getNum()));
				} else if (node.getMode().equals(Mode.getM()) && node.getPred().equals(toInline.getStart())) {
					// memory Proj from start
					calleeToCallerCopied.put(node, call.getMem());
				} else {
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
				}

				binding_irnode.set_irn_in(oldBlock.ptr, jumps.length, Node.getBufferFromNodeList(jumps));

				Map<Mode, List<Node>> phis = new HashMap<>();
				for (Return ret : returns) {
					for (Node pred : ret.getPreds()) {
						phis.computeIfAbsent(pred.getMode(), ignored -> new ArrayList<>()).add(calleeToCallerCopied.get(pred));
					}
				}
				for (Map.Entry<Mode, List<BackEdges.Edge>> entry : users.entrySet()) {
					List<Node> ins = phis.get(entry.getKey());
					Node phi = graph.newPhi(oldBlock, ins.toArray(Node[]::new), entry.getKey());
					for (BackEdges.Edge userEdge : entry.getValue()) {
						userEdge.node.setPred(userEdge.pos, phi);
					}
				}
/*				for (Return ret : returns) {
					for (Node pred : ret.getPreds()) {
						Node newPred = calleeToCallerCopied.get(pred);
						LOGGER.debug("pred %s, return %s map %s", pred, ret, calleeToCallerCopied);
						// precalculated edges
						for (BackEdges.Edge user : users.getOrDefault(newPred.getMode(), List.of())) {
							LOGGER.debug("rewire %s to %s", user.node, newPred);
							user.node.setPred(user.pos, newPred);
						}
					}
				}*/
			}

			@Override
			public void visit(Block old) {
				if (old.getGraph().getStartBlock().equals(old)) {
					// call.getBlock() is newly created block
					calleeToCallerCopied.put(old, call.getBlock());
					return;
				}
				Node copyBlock = old.copyInto(graph);
				LOGGER.debug("copying %s to %s", old, copyBlock);
				calleeToCallerCopied.put(old, copyBlock);
				for (int i = 0; i < old.getPredCount(); i++) {
					// set the new pred
					Node newNode = calleeToCallerCopied.get(old.getPred(i));
					if (newNode == null) {
						newNode = graph.newUnknown(old.getPred(i).getMode());
					}
					copyBlock.setPred(i, newNode);
				}
			}

			@Override
			public void defaultVisit(Node old) {
				Node copy = old.copyInto(graph);
				copy.setBlock(calleeToCallerCopied.get(old.getBlock()));
				calleeToCallerCopied.put(old, copy);
				for (int i = 0; i < old.getPredCount(); i++) {
					// set the new pred
					Node newNode = calleeToCallerCopied.get(old.getPred(i));
					if (newNode == null) {
						newNode = graph.newUnknown(old.getPred(i).getMode());
					}
					copy.setPred(i, newNode);
				}
			}
		});
		LOGGER.debug("inlined %s into %s", toInline, graph);
		LOGGER.debug("enabled = %s", BackEdges.enabled(graph));
	}

	private Map<Mode, List<BackEdges.Edge>> findUsers(Call call) {
		LOGGER.debug("enable for %s", call.getGraph());
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
					LOGGER.debug("found user %s", edge.node);
					users.computeIfAbsent(next.getMode(), ignored -> new ArrayList<>()).add(edge);
				}
			}
		}
		LOGGER.debug("all %s", users);
		LOGGER.debug("disable for %s", call.getGraph());
		BackEdges.disable(call.getGraph());
		return users;
	}

	private Node splitBlockAt(Call call) {
		Graph graph = call.getGraph();
		Node oldBlock = call.getBlock();
		Node newBlock = graph.newBlock(predArray(oldBlock));
		if (graph.getStartBlock().equals(oldBlock)) {
			graph.setStartBlock((Block) newBlock);
		}
		movePredecessors(call, oldBlock, newBlock);
		return oldBlock;
	}

	private void movePredecessors(Node node, Node oldBlock, Node newBlock) {
		node.setBlock(newBlock);
		if (node instanceof Phi) {
			return;
		}
		for (Node pred : node.getPreds()) {
			if (pred.getBlock().equals(oldBlock)) {
				movePredecessors(pred, oldBlock, newBlock);
			}
		}
	}

	private Node[] predArray(Node node) {
		Node[] nodes = new Node[node.getPredCount()];
		for (int i = 0; i < node.getPredCount(); i++) {
			nodes[i] = node.getPred(i);
		}
		return nodes;
	}

	private List<InlineCandidate> findInlineCandidates(Graph graph, Map<Graph, Double> candidates) {
		List<InlineCandidate> result = new ArrayList<>();
		Set<Call> calls = callGraph.callSitesIn(graph);
		for (Call call : calls) {
			Graph called = ((Address) call.getPtr()).getEntity().getGraph();
			if (called != null) { // is null for runtime calls
				double cost = candidates.getOrDefault(called, Double.MAX_VALUE);
				result.add(new InlineCandidate(call, called, cost));
			}
		}
		return result;
	}

	record InlineCandidate(
		Call call,
		Graph graph,
		double cost
	) {

	}

	// TODO
	// Should be aware of:
	// - memory
	// - loops
	// 
	static class CostCalculator extends NodeVisitor.Default {
		private final Graph graph;
		private double cost;
		private final Map<Node, Double> costMap;

		CostCalculator(Graph graph) {
			this.graph = graph;
			this.costMap = new HashMap<>();
			this.cost = Double.NaN; // lazy, with NaN as invalid value
			graph.walk(this);
		}

		public double cost() {
			if (Double.isNaN(cost)) {
				cost = costMap.values().stream().mapToDouble(Double::doubleValue).sum();
			}
			return cost;
		}

		public double costOf(Node node) {
			return costMap.getOrDefault(node, 0d); // TODO defaultValue needed?
		}

		@Override
		public void defaultVisit(Node n) {
			costMap.put(n, 1d);
		}
	}
}
