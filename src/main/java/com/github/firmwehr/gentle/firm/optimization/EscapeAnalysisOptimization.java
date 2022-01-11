package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.construction.StdLibEntity;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.Add;
import firm.nodes.Address;
import firm.nodes.Call;
import firm.nodes.Cmp;
import firm.nodes.Const;
import firm.nodes.Load;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Store;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.util.JImmutables;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

public class EscapeAnalysisOptimization {
	private static final Logger LOGGER = new Logger(EscapeAnalysisOptimization.class);

	private final Graph graph;

	private EscapeAnalysisOptimization(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> escapeAnalysisOptimization() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("EscapeAnalysisOptimization")
			.withOptimizationFunction(graph -> {
				int runs = 0;
				while (true) {
					EscapeAnalysisOptimization optimization = new EscapeAnalysisOptimization(graph);
					if (optimization.optimize()) {
						runs++;
					} else {
						return runs > 0;
					}
				}
			})
			.build();
	}

	public boolean optimize() {
		LOGGER.debug("Scanning %s", graph.getEntity().getLdName());
		Set<Call> allocationCalls = new HashSet<>();
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				if (!((Address) node.getPtr()).getEntity().equals(StdLibEntity.ALLOCATE.getEntity())) {
					return;
				}

				if (!(node.getPred(2) instanceof Const)) {
					return; // dynamically sized array, would be somewhat more difficult to deal with correctly
				}
				filterNonEscapingAllocations(node).ifPresent(allocationCalls::add);
			}
		});
		rewriteAll(allocationCalls);
		BackEdges.disable(graph);
		// if the set is not empty, we definitely changed something
		if (!allocationCalls.isEmpty()) {
			GraphDumper.dumpGraph(graph, "escape-analysis");
			return true;
		}
		return false;
	}

	private void rewriteAll(Set<Call> allocationCalls) {
		for (Call call : allocationCalls) {
			rewrite(call);
		}
	}

	private void dropAllocation(Call call) {
		Node memoryProj = successor(call, true);
		for (BackEdges.Edge edge : BackEdges.getOuts(memoryProj)) {
			edge.node.setPred(edge.pos, call.getMem());
		}
	}

	private void rewrite(Call call) {
		LOGGER.info("rewriting loads and stores for %s", call);
		Set<Node> accessNodes = new HashSet<>();
		Map<LocalAddress, Mode> modes = new HashMap<>();
		graph.incVisited(); // increment for walkDown
		walkDown(call, new NodeVisitor.Default() {
			@Override
			public void visit(Load node) {
				remember(node, node.getPtr(), successor(node, false));
			}

			@Override
			public void visit(Store node) {
				remember(node, node.getPtr(), node.getValue());
			}

			private void remember(Node node, Node ptr, Node modeHolder) {
				LocalAddress localAddress = fromPred(ptr);
				LOGGER.debug("remember %s with ptr %s (mode %s) for %s", node, ptr, modeHolder, localAddress);
				accessNodes.add(node);
				modes.putIfAbsent(localAddress, modeHolder.getMode());
			}
		});
		new CallRewriter(call, accessNodes, modes).rewrite();
		// actually remove the call
		dropAllocation(call);
	}

	private static Node successor(Node node, boolean memory) {
		if (node instanceof Proj) {
			for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
				return edge.node;
			}
		}
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			if (edge.node.getMode().equals(Mode.getM()) == memory) {
				return edge.node;
			}
		}
		LOGGER.debug("%s has %s outs without memory", node, BackEdges.getNOuts(node));
		throw new InternalCompilerException("No successor found for " + node);
	}

	private static LocalAddress fromPred(Node pred) {
		return fromPredIfMatches(pred).orElseThrow(
			() -> new InternalCompilerException("Don't know how to transform " + pred + " into local address"));
	}

	private static Optional<LocalAddress> fromPredIfMatches(Node pred) {
		if (pred instanceof Proj proj) {
			return Optional.of(new LocalAddress(proj, 0)); // 0th field in class
		} else if (pred instanceof Add add) {
			if (add.getLeft() instanceof Proj proj && add.getRight() instanceof Const val) {
				return Optional.of(new LocalAddress(proj, val.getTarval().asLong()));
			} else if (add.getRight() instanceof Proj proj && add.getLeft() instanceof Const val) {
				return Optional.of(new LocalAddress(proj, val.getTarval().asLong()));
			}
		}
		return Optional.empty();
	}

	private static void walkDown(Node node, NodeVisitor visitor) {
		node.accept(visitor);
		node.markVisited();
		if (node instanceof Load || node instanceof Store) {
			// we don't need to follow Loads and Stores as their successors
			// don't use the call result (and if they do, they are visited separately)
			return;
		}
		for (BackEdges.Edge out : BackEdges.getOuts(node)) {
			if ((out.node.getMode().equals(Mode.getT()) || out.node.getMode().equals(Mode.getP())) &&
				!out.node.visited()) {
				LOGGER.debug("visiting %s", out.node);
				walkDown(out.node, visitor);
			} else {
				LOGGER.debug("not visiting %s (visited = %s)", out.node, out.node.visited());
			}
		}
	}

	// return calls that do not escape
	private Optional<Call> filterNonEscapingAllocations(Call allocationCall) {
		if (BackEdges.getNOuts(allocationCall) < 2) {
			return Optional.of(allocationCall);
		}
		Node projResT = successor(allocationCall, false);
		Proj resultProj = (Proj) successor(projResT, false);
		if (!escapes(resultProj)) {
			return Optional.of(allocationCall);
		}
		return Optional.empty();
	}

	private boolean escapes(Proj callResProj) {
		Deque<Node> deepOuts = new ArrayDeque<>();
		deepOuts.add(callResProj);
		while (!deepOuts.isEmpty()) {
			Node next = deepOuts.removeLast();
			for (BackEdges.Edge edge : BackEdges.getOuts(next)) {
				if (edge.node.getMode().equals(Mode.getM())) {
					LOGGER.debug("stop at %s (memory)", edge.node);
					continue;
				}
				if (edge.node instanceof Call || edge.node instanceof Return) {
					LOGGER.debug("%s escapes on %s", callResProj, edge.node);
					return true;
				}
				if (edge.node instanceof Load load) {
					if (fromPredIfMatches(load.getPtr()).isPresent()) {
						// Load from this call => ignore (would be replaced)
						LOGGER.debug("stop at %s", edge.node);
						continue;
					}
					LOGGER.debug("%s escapes on %s (invalid address)", callResProj, edge.node);
					return true;
				}
				if (edge.node instanceof Store store) {
					if (!callResProjIsReachableFromStoreValue(store.getValue(), callResProj, new HashSet<>())) {
						if (fromPredIfMatches(store.getPtr()).isPresent()) {
							// Store *in* allocated object would be replaced
							LOGGER.debug("stop at %s", edge.node);
							continue;
						}
					}
					// a) store in other object, mark this as escape. If other object does not escape, this
					//    can be optimized when running escape analysis again
					// b) the store address is in an unknown format, we can't rewrite it.
					//    In that case, we mark it as escaped
					LOGGER.debug("%s escapes on %s", callResProj, edge.node);
					return true;
				}
				if (edge.node instanceof Cmp) {
					// we don't have a pointer to compare, but this should be optimized away
					// in a different step anyway
					LOGGER.debug("%s is used in %s", next, edge.node);
					return true;
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
		LOGGER.info("%s does not escape, can be optimized", callResProj);
		return false;
	}

	private boolean callResProjIsReachableFromStoreValue(Node storePred, Proj callResProj, Set<Node> seen) {
		if (!seen.add(storePred)) {
			return false;
		}
		if (storePred.equals(callResProj)) {
			return true;
		}
		for (Node pred : storePred.getPreds()) {
			// load does not escape
			// store would have been checked separately already
			if (pred instanceof Load || pred instanceof Store) {
				continue;
			}
			// memory nodes don't lead to callResProj (as it is a value)
			if (!pred.getMode().equals(Mode.getM()) && callResProjIsReachableFromStoreValue(pred, callResProj, seen)) {
				return true;
			}
		}
		return false;
	}

	static class CallRewriter {

		private final Call call;
		private final Graph graph;
		private final Map<LocalAddress, Mode> modes;
		private final Map<Phi, Map<LocalAddress, Phi>> phis = new HashMap<>();
		private final Set<Node> interestingNodes;
		private final List<Runnable> replacements;

		CallRewriter(Call call, Set<Node> interestingNodes, Map<LocalAddress, Mode> modes) {
			this.call = call;
			this.graph = call.getGraph();
			this.modes = modes;
			this.interestingNodes = interestingNodes;
			this.replacements = new ArrayList<>();
		}

		public void rewrite() {
			graph.incVisited(); // increment for walkRecursive
			Map<LocalAddress, Node> predecessors = modes.entrySet()
				.stream()
				.collect(toMap(Map.Entry::getKey, entry -> graph.newConst(entry.getValue().getNull())));
			walkRecursive(call, -1, JImmutables.map(predecessors));
			for (Runnable runnable : replacements) {
				runnable.run();
			}
		}

		private void walkRecursive(Node node, int inIndex, JImmutableMap<LocalAddress, Node> predecessors) {
			if (node.visited()) {
				LOGGER.debug("skip %s", node);
				return;
			}
			LOGGER.debug("visit %s", node);
			JImmutableMap<LocalAddress, Node> follow = predecessors;
			if (node instanceof Phi memoryPhi) {
				Node block = node.getBlock();
				for (LocalAddress address : modes.keySet()) {
					Node pred = predecessors.get(address);
					Phi dataPhi;
					if (phis.containsKey(memoryPhi) && phis.get(memoryPhi).containsKey(address)) {
						LOGGER.debug("load data phi for memory phi %s and address %s", memoryPhi, address);
						dataPhi = phis.get(memoryPhi).get(address);
					} else {
						Mode mode = modes.get(address);
						Node[] ins = createUnknowns(memoryPhi.getPredCount(), mode);
						dataPhi = (Phi) graph.newPhi(block, ins, mode);
						LOGGER.debug("insert new data phi %s for %s", dataPhi, address);
						phis.computeIfAbsent(memoryPhi, ignored -> new HashMap<>()).putIfAbsent(address, dataPhi);
					}
					dataPhi.setPred(inIndex, pred);
					follow = follow.assign(address, dataPhi);
				}
			} else if (node instanceof Load load && interestingNodes.contains(load)) {
				LocalAddress address = fromPred(load.getPtr());
				Node value = follow.get(address);
				replacements.add(() -> replaceLoad(load, value));
			} else if (node instanceof Store store && interestingNodes.contains(store)) {
				follow = follow.assign(fromPred(store.getPtr()), store.getValue());
				replacements.add(() -> replaceStore(store));
			}
			if (!(node instanceof Phi)) {
				node.markVisited();
			}
			if (node.getMode().equals(Mode.getM())) {
				for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
					LOGGER.debug("(M) edge with node %s", edge.node);
					if (!edge.node.getMode().equals(Mode.getX())) {
						walkRecursive(edge.node, edge.pos, follow);
					}
				}
			} else if (node.getMode().equals(Mode.getT())) {
				for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
					LOGGER.debug("(T) edge with node %s", edge.node);
					if (edge.node.getMode().equals(Mode.getM())) {
						walkRecursive(edge.node, edge.pos, follow);
					}
				}
			} else {
				throw new InternalCompilerException("Illegal node visited %s".formatted(node));
			}
		}

		private void replaceStore(Store store) {
			Node memoryProj = successor(store, true);
			for (BackEdges.Edge edge : BackEdges.getOuts(memoryProj)) {
				edge.node.setPred(edge.pos, store.getMem());
			}
		}

		private Node[] createUnknowns(int predCount, Mode mode) {
			Node[] nodes = new Node[predCount];
			for (int i = 0; i < predCount; i++) {
				nodes[i] = graph.newUnknown(mode);
			}
			return nodes;
		}

		private void replaceLoad(Load load, Node value) {
			Node dataProj = successor(load, false);
			for (BackEdges.Edge edge : BackEdges.getOuts(dataProj)) {
				edge.node.setPred(edge.pos, value);
			}
			Node memoryProj = successor(load, true);
			for (BackEdges.Edge edge : BackEdges.getOuts(memoryProj)) {
				edge.node.setPred(edge.pos, load.getMem());
			}
		}
	}

	/**
	 * A field of an object, identified by a base pointer and the offset within the object.
	 */
	record LocalAddress(
		Proj pointer,
		long offset
	) {
	}
}
