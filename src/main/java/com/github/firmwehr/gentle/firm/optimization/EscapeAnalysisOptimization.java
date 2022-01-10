package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.construction.StdLibEntity;
import com.github.firmwehr.gentle.output.Logger;
import com.github.firmwehr.gentle.util.GraphDumper;
import com.google.common.base.Preconditions;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.TargetValue;
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
import firm.nodes.Unknown;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EscapeAnalysisOptimization {
	private static final Logger LOGGER = new Logger(EscapeAnalysisOptimization.class, Logger.LogLevel.DEBUG);

	private final Graph graph;

	public EscapeAnalysisOptimization(Graph graph) { // TODO private
		this.graph = graph;
	}

	public boolean optimize() {
		LOGGER.debug("Scanning %s", graph.getEntity().getLdName());
		Set<Call> allocationCalls = new HashSet<>();
		BackEdges.enable(graph);
		graph.walk(new NodeVisitor.Default() {
			@Override
			public void visit(Call node) {
				// TODO arrays might be 1 element or 0 element?
				if (!(node.getPred(2) instanceof Const) || ((Const) node.getPred(2)).getTarval().asLong() > 1L) {
					return; // array, would be somewhat more difficult to deal with correctly
				}
				if (((Address) node.getPtr()).getEntity().equals(StdLibEntity.ALLOCATE.getEntity())) {
					filterNonEscapingAllocations(node).ifPresent(allocationCalls::add);
				}
			}
		});
		rewriteAll(allocationCalls);
		BackEdges.disable(graph);
		GraphDumper.dumpGraph(graph, "escape-analysis"); // TODO only dump on change
		return !allocationCalls.isEmpty(); // TODO ?
	}

	private void rewriteAll(Set<Call> allocationCalls) {
		for (Call call : allocationCalls) {
			rewrite(call);
		}
	}

	private void dropAllocation(Call call) {
		Node mem = skipProj(call.getMem());
		Node proj = successor(call, true);
		proj.setPred(Call.pnM, mem);
	}

	private void rewrite(Call call) {
		LOGGER.debug("rewriting loads and stores for %s", call);
		Map<LocalAddress, List<Node>> accesses = new HashMap<>(); // read/write access to the memory allocated by call
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
				accesses.computeIfAbsent(localAddress, ignored -> new ArrayList<>()).add(node);
				modes.putIfAbsent(localAddress, modeHolder.getMode());
			}
		});
		graph.incVisited(); // increment for walkMemory
		Map<LocalAddress, Node> predecessors = new HashMap<>();
		Set<Node> accessNodes = accesses.values().stream().flatMap(List::stream).collect(Collectors.toSet());
		Map<Phi, Map<LocalAddress, Node>> phis = new HashMap<>();
		walkMemory(call, new NodeVisitor.Default() {

			@Override
			public void visit(Phi node) {
				Node block = node.getBlock();
				for (Map.Entry<LocalAddress, List<Node>> entry : accesses.entrySet()) {
					LocalAddress address = entry.getKey();
					Mode mode = modes.get(address);
					Node placeholder = graph.newUnknown(mode);
					Node pred = calculatePred(address, mode, predecessors);
					Node phi = graph.newPhi(block, new Node[]{placeholder, pred}, mode);
					phis.computeIfAbsent(node, ignored -> new HashMap<>()).put(address, phi);
					updatePredecessor(predecessors, address, phi);
				}
			}

			@Override
			public void visit(Load node) {
				// make sure this Load is interesting for us
				if (!accessNodes.contains(node)) {
					return;
				}
				LOGGER.debug("%s needs to be replaced", node);
				LocalAddress localAddress = fromPred(node.getPtr());
				Node dataProj = successor(node, false);
				Node pred = calculatePred(localAddress, dataProj.getMode(), predecessors);
				rewireMemory(node);
				Node user = successor(dataProj, false);
				for (int i = 0; i < user.getPredCount(); i++) {
					if (user.getPred(i).equals(dataProj)) {
						LOGGER.debug("set pred of %s to %s (%s)", user, pred, node);
						user.setPred(i, pred);
					}
				}
			}

			@Override
			public void visit(Store node) {
				// make sure this Load is interesting for us
				if (!accessNodes.contains(node)) {
					LOGGER.debug("ignoring %s, not accessed", node);
					return;
				}
				LOGGER.debug("%s needs to be replaced", node);
				// assuming this is a store *into* our object we want to inline
				LocalAddress localAddress = fromPred(node.getPtr());
				updatePredecessor(predecessors, localAddress, node.getValue());
				rewireMemory(node) // collect outgoing memory phis
					.map(phis::get) // map them to our created data phis
					.filter(Objects::nonNull) // not all memory phis have corresponding data phis
					.map(map -> map.get(localAddress)) //
					.forEach(n -> {
						for (int i = 0; i < n.getPredCount(); i++) {
							if (n.getPred(i) instanceof Unknown) {
								n.setPred(i, node.getValue());
							}
						}
					});
			}
		});
		// actually remove the call
		dropAllocation(call);
	}

	private void walkMemoryRecursive(Call call) {

	}

	@NotNull
	private Node calculatePred(LocalAddress localAddress, Mode mode, Map<LocalAddress, Node> predecessors) {
		Node pred = predecessors.get(localAddress);
		if (pred == null) {
			// no store before, fallback to default value
			pred = graph.newConst(new TargetValue(0, mode));
			pred.markVisited(); // skip in walkMemory
			updatePredecessor(predecessors, localAddress, pred);
		}
		return pred;
	}

	private static void updatePredecessor(Map<LocalAddress, Node> predecessors, LocalAddress localAddress,
	                                      Node value) {
		LOGGER.debug("update predecessor to %s (was %s)", value, predecessors.get(localAddress));
		predecessors.put(localAddress, value);
	}

	private static void walkMemory(Node node, NodeVisitor visitor) {
		// first: collect nodes, they might be rewritten when visiting
		// second: visit and rewrite
		Deque<Node> queue = new ArrayDeque<>();
		List<Node> result = new ArrayList<>();
		queue.add(node);
		while (!queue.isEmpty()) {
			Node top = queue.remove();
			top.markVisited();
			result.add(top);
			if (top.getMode().equals(Mode.getM())) {
				for (BackEdges.Edge edge : BackEdges.getOuts(top)) {
					if (!edge.node.getMode().equals(Mode.getX()) && !edge.node.visited()) {
						queue.add(edge.node);
					}
				}
			} else if (top.getMode().equals(Mode.getT())) {
				for (BackEdges.Edge edge : BackEdges.getOuts(top)) {
					if (edge.node.getMode().equals(Mode.getM()) && !edge.node.visited()) {
						queue.add(edge.node);
					}
				}
			}
		}
		for (Node memNode : result) {
			memNode.accept(visitor);
		}
	}

	private Node successor(Node node, boolean memory) {
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
		GraphDumper.dumpGraph(graph, "bug");
		throw new InternalCompilerException("No successor found for " + node);
	}

	private Stream<Phi> rewireMemory(Node node) {
		Preconditions.checkArgument(node.getMode().equals(Mode.getT()), "expected Mode T for %s", node);
		// TODO all outs of Proj
		Node pred = node.getPred(Load.pnM); // Phi or Proj M
		Node proj = successor(node, true);
		List<Phi> outPhis = new ArrayList<>();
		for (BackEdges.Edge edge : BackEdges.getOuts(proj)) {
			Node memNode = edge.node;
			LOGGER.debug("replace memory with path from %s to %s", pred, memNode);
			if (memNode instanceof Phi phi) {
				// find the correct pred of this phi that should be rewired
				for (int i = 0; i < phi.getPredCount(); i++) {
					if (phi.getPred(i).equals(proj)) {
						phi.setPred(i, pred);
					}
				}
				outPhis.add(phi);
			} else {
				memNode.setPred(Load.pnM, pred); // Load.pnM == Store.pnM == Proj.pnMax
			}
		}
		return outPhis.stream();
	}

	private Node skipProj(Node mem) {
		if (mem instanceof Proj proj) {
			return skipProj(proj.getPred());
		}
		return mem;
	}

	private static LocalAddress fromPred(Node pred) {
		if (pred instanceof Proj proj) {
			return new LocalAddress(proj, 0); // 0th field in class
		} else if (pred instanceof Add add) {
			if (add.getLeft() instanceof Proj proj && add.getRight() instanceof Const val) {
				return new LocalAddress(proj, val.getTarval().asLong());
			} else if (add.getRight() instanceof Proj proj && add.getLeft() instanceof Const val) {
				return new LocalAddress(proj, val.getTarval().asLong());
			}
		}
		throw new InternalCompilerException("Don't know how to transform " + pred + " into local address");
	}

	private static void walkDown(Node node, NodeVisitor visitor) {
		node.accept(visitor);
		node.markVisited();
		// TODO stop on Load/Store
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
					// TODO what happens with self ref?
					if (storeInMemoryProvidedByCall(store.getPtr(), call)) {
						// Store *in* allocated object would be replaced
						LOGGER.debug("stop at %s", edge.node);
						continue;
					}
					// store in other object, mark this as escape. If other object does not escape, this
					// can be optimized when running escape analysis again
					LOGGER.debug("%s escapes on %s", call, edge.node);
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
		LOGGER.debug("%s does not escape, can be optimized", call);
		return false;
	}

	private boolean storeInMemoryProvidedByCall(Node storePred, Call call) {
		if (storePred.equals(call)) {
			return true;
		}
		for (Node pred : storePred.getPreds()) {
			if (storeInMemoryProvidedByCall(pred, call)) {
				return true;
			}
		}
		return false;
	}

	record LocalAddress(
		Proj pointer,
		long offset
	) {
	}

	interface StateNodeVisitor<S> {
		S visit(Load load, S state);

		S visit(Phi phi, S state);

		S visit(Store store, S state);
	}
}
