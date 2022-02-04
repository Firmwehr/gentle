package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.firm.optimization.memory.LocalAddress;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.github.firmwehr.gentle.util.Pair;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.Add;
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

/**
 * Scalar replacement of allocations that do not escape the method scope.
 * <p>
 * This is done by filtering for allocations where the object is <b>never</b>:
 * <ul>
 *     <li>stored</li>
 *     <li>passed to a method call</li>
 *     <li>returned</li>
 * </ul>
 * <p>
 * As an edge case, object allocation is not replaced if the pointer is input of a comparison.
 * Additionally, an object is considered as escaping if
 * <ul>
 * <li>stores into or loads from that object have dynamic offsets.</li>
 * <li>the object size is dynamic.</li>
 * </ul>
 * <p>
 * Different fields in an object are represented as {@link LocalAddress} which contains the base pointer
 * and the offset.
 * <p>
 * For allocations that meet the named criteria, all Loads and Stores are replaced by
 * maintaining predecessor nodes for the given {@link LocalAddress} (initialised to their default values).
 * The predecessors are updated on Stores and used on Loads.
 * <p>
 * Memory Phis are used to determine where data Phis are required. Each time a memory Phi is visited,
 * a new input of corresponding data Phis can be set.
 */
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
				if (!Util.isAllocCall(node)) {
					return;
				}

				if (!(node.getPred(2) instanceof Const)) {
					return; // dynamically sized array, would be somewhat more difficult to deal with correctly
				}
				if (!escapes(node)) {
					allocationCalls.add(node);
				}
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
				remember(node, node.getPtr(), successor(node, false), fromLoadUnchecked(node));
			}

			@Override
			public void visit(Store node) {
				remember(node, node.getPtr(), node.getValue(), fromStoreUnchecked(node));
			}

			private void remember(Node node, Node ptr, Node modeHolder, LocalAddress localAddress) {
				LOGGER.debug("remember %s with ptr %s (mode %s) for %s", node, ptr, modeHolder, localAddress);
				accessNodes.add(node);
				modes.putIfAbsent(localAddress, modeHolder.getMode());
			}
		});
		new CallRewriter(call, accessNodes, modes).rewrite();
		// actually remove the call
		dropAllocation(call);
	}

	/**
	 * Returns the memory successor if {@code memory} is true, or the first non-memory successor otherwise. This should
	 * only be used if exactly one successor of each kind exists.
	 */
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
		GraphDumper.dumpGraph(node.getGraph(), "missing-successor");
		throw new InternalCompilerException("No successor found for " + node);
	}

	private static LocalAddress fromLoadUnchecked(Load load) {
		return LocalAddress.fromLoad(load)
			.orElseThrow(() -> new InternalCompilerException(
				"Don't know how to transform " + load.getPtr() + " into local address"));
	}

	private static LocalAddress fromStoreUnchecked(Store store) {
		return LocalAddress.fromStore(store)
			.orElseThrow(() -> new InternalCompilerException(
				"Don't know how to transform " + store.getPtr() + " into local address"));
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
			// when walking down from the call, we only need to consider tuple nodes (Load, Store)
			// or pointer nodes (Proj). Memory nodes don't bring us more relevant information here
			// as the data nodes will always lead to all its usages
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
	private boolean escapes(Call allocationCall) {
		if (BackEdges.getNOuts(allocationCall) < 2) {
			return false;
		}
		Node projResT = successor(allocationCall, false);
		Proj resultProj = (Proj) successor(projResT, false);
		return escapes(resultProj);
	}

	private boolean escapes(Proj callResProj) {
		// DFS, keep track of the nodes we need to visit
		Deque<Node> deepOuts = new ArrayDeque<>();
		// maintain visited set for recursive Phis
		// we cannot use Node#markVisited() as this method is called
		// during Visitor walk
		Set<Node> visited = new HashSet<>();
		deepOuts.add(callResProj);
		while (!deepOuts.isEmpty()) {
			Node next = deepOuts.removeLast();
			if (!visited.add(next)) {
				continue;
			}
			for (BackEdges.Edge edge : BackEdges.getOuts(next)) {
				if (edge.node.getMode().equals(Mode.getM())) {
					// we want to ignore memory nodes, otherwise we'll visit a lot of unrelated nodes
					// we reach every related Load/Store/Cmp/call through data nodes
					LOGGER.debug("stop at %s (memory)", edge.node);
					continue;
				}
				if (edge.node instanceof Call || edge.node instanceof Return) {
					LOGGER.debug("%s escapes on %s", callResProj, edge.node);
					return true;
				}
				if (edge.node instanceof Load load) {
					if (LocalAddress.fromLoad(load).isEmpty()) {
						LOGGER.debug("%s escapes on %s (invalid address)", callResProj, edge.node);
						return true;
					} else if (BackEdges.getNOuts(edge.node) <= 1) {
						// unused loads will be removed by FirmGraphCleanup, then we can probably inline it :)
						LOGGER.debug("%s escapes on %s (no data proj out)", callResProj, edge.node);
						return true;
					} else {
						// Load from this call => ignore (would be replaced)
						LOGGER.debug("stop at %s", edge.node);
						continue;
					}
				}
				if (edge.node instanceof Store store) {
					// if call is reachable from the value that is stored here
					// when walking up the predecessors -> case a)
					if (!callResProjIsReachableFromStoreValue(store.getValue(), callResProj, new HashSet<>())) {
						// if address is dynamic/in an unknown format -> case b)
						if (LocalAddress.fromStore(store).isPresent()) {
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
				// a node that can't be ignored
				LOGGER.debug("add %s to outs to scan", edge.node);
				deepOuts.add(edge.node);
			}
		}
		LOGGER.info("%s does not escape, can be optimized", callResProj);
		return false;
	}

	/**
	 * Walks up the predecessors of the stored value to see if the call is reachable through data nodes. The
	 * {@code seen} set is required to avoid infinite recursion caused by data phis.
	 */
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

	/**
	 * Does the actual replacement by keeping the state of each scalar value through Stores, Loads and Phis.
	 * <p>
	 * Instances of this class are used per call.
	 */
	static class CallRewriter {

		private final Call call; // the allocation call
		private final Graph graph;
		private final Map<LocalAddress, Mode> modes; // all local addresses and their modes
		private final Map<Phi, Map<LocalAddress, Phi>> phis; // memory Phis -> data Phis per address
		private final Set<Node> interestingNodes; // Loads and Stores that need to be replaced
		private final List<Runnable> replacements; // tasks that delete from the graph need to be delayed
		private final Set<Pair<Phi, Integer>> visitedPhiIncomingEdges;
		private final ForwardChain forwardChain;

		CallRewriter(Call call, Set<Node> interestingNodes, Map<LocalAddress, Mode> modes) {
			this.call = call;
			this.graph = call.getGraph();
			this.modes = modes;
			this.phis = new HashMap<>();
			this.interestingNodes = interestingNodes;
			this.replacements = new ArrayList<>();
			this.visitedPhiIncomingEdges = new HashSet<>();
			this.forwardChain = new ForwardChain();
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

		/**
		 * @param node the node we want to visit
		 * @param inIndex the index of the pred we're coming from.
		 * @param predecessors the state of predecessors in the current path
		 */
		private void walkRecursive(Node node, int inIndex, JImmutableMap<LocalAddress, Node> predecessors) {
			if (node.visited()) {
				LOGGER.debug("skip %s", node);
				return;
			}
			if (node instanceof Phi phi) {
				// we need to visit Phis multiple times, but each of their outs only once
				// as Phis can be self-referencing, we mark the incoming edges as visited
				if (!visitedPhiIncomingEdges.add(new Pair<>(phi, inIndex))) {
					LOGGER.debug("skip repeated in for phi %s", node);
					return;
				}
			} else {
				node.markVisited();
			}
			LOGGER.debug("visit %s", node);
			JImmutableMap<LocalAddress, Node> follow = predecessors;
			if (node instanceof Phi memoryPhi) {
				Node block = node.getBlock();
				// for each local address, we (might) need a data Phi
				for (LocalAddress address : modes.keySet()) {
					Node pred = predecessors.get(address);
					Phi dataPhi; // data Phi corresponding to the memory Phi for the given address
					if (phis.containsKey(memoryPhi) && phis.get(memoryPhi).containsKey(address)) {
						LOGGER.debug("load data phi for memory phi %s and address %s", memoryPhi, address);
						dataPhi = phis.get(memoryPhi).get(address);
					} else {
						// if the Phi does not exist, we're visiting the memory Phi the first time.
						// we create a Phi with n Unknowns as predecessors, where n is the number
						// of inputs of the memory Phi (as this is the amount of times we'll come
						// back again
						Mode mode = modes.get(address);
						Node[] ins = createUnknowns(memoryPhi.getPredCount(), mode);
						dataPhi = (Phi) graph.newPhi(block, ins, mode);
						LOGGER.debug("insert new data phi %s for %s", dataPhi, address);
						phis.computeIfAbsent(memoryPhi, ignored -> new HashMap<>()).putIfAbsent(address, dataPhi);
					}
					// set pred at the index of the in index of the memory Phi to the current pred
					dataPhi.setPred(inIndex, pred);
					// further replacements of Loads should use this Phi as pred
					LOGGER.debug("update pred %s -> %s", address, dataPhi);
					follow = follow.assign(address, dataPhi);
				}
			} else if (node instanceof Load load && interestingNodes.contains(load)) {
				LocalAddress address = fromLoadUnchecked(load);
				// load the last value that would have been stored (including Phis)
				Node value = follow.get(address);
				LOGGER.debug("remember replacement %s -> %s", load, value);
				replacements.add(() -> replaceLoad(load, value));
			} else if (node instanceof Store store && interestingNodes.contains(store)) {
				// assign to the value that would have been stored, so the next Load/Phi can use it
				LOGGER.debug("update pred %s -> %s", store, store.getValue());
				follow = follow.assign(fromStoreUnchecked(store), store.getValue());
				replacements.add(() -> replaceStore(store));
			}

			if (node.getMode().equals(Mode.getM())) {
				for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
					LOGGER.debug("(M) edge with node %s", edge.node);
					if (!edge.node.getMode().equals(Mode.getX())) { // ignore control flow, e.g. Return
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

		/**
		 * Strip the Store from the memory chain by setting the predecessor of the Store's successor to the Store's
		 * predecessor
		 */
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

		/**
		 * Rewire the users of the loaded data to use the given value instead. Also, strip the Load from the memory
		 * chain by setting the predecessor of the Load's successor to the Load's predecessor
		 */
		private void replaceLoad(Load load, Node value) {
			Node dataProj = successor(load, false);
			Node newValue = forwardChain.getForwarder(value);
			forwardChain.rememberForward(dataProj, newValue);
			for (BackEdges.Edge edge : BackEdges.getOuts(dataProj)) {
				edge.node.setPred(edge.pos, newValue);
			}
			Node memoryProj = successor(load, true);
			for (BackEdges.Edge edge : BackEdges.getOuts(memoryProj)) {
				edge.node.setPred(edge.pos, load.getMem());
			}
		}
	}

	/**
	 * When replacing, the stored pred might be already replaced (load 1 -> store  2 -> load 3). If load 1 is replaced
	 * before load 3, the value stored at 2 does still point to the old successor of load 1. We need to rewire it to
	 * the
	 * replaced value of load 1.
	 */
	private static class ForwardChain {
		private final Map<Node, Node> forwards = new HashMap<>();

		public void rememberForward(Node victim, Node murderer) {
			forwards.put(victim, murderer);
		}

		public Node getForwarder(Node node) {
			if (!forwards.containsKey(node)) {
				return node;
			}
			return getForwarder(forwards.get(node));
		}
	}
}
