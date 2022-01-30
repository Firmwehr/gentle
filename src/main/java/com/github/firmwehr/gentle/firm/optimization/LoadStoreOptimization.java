package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.fiascii.FiAscii;
import com.github.firmwehr.fiascii.generated.LoadAndLoadPattern;
import com.github.firmwehr.fiascii.generated.StoreAndLoadPattern;
import com.github.firmwehr.fiascii.generated.StoreAndStorePattern;
import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.Util;
import com.github.firmwehr.gentle.output.Logger;
import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.Div;
import firm.nodes.Load;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Return;
import firm.nodes.Store;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class LoadStoreOptimization extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(LoadStoreOptimization.class, Logger.LogLevel.DEBUG);
	private final Graph graph;
	private boolean changed;

	private static final OptimizationList OPTIMIZATIONS = OptimizationList.builder()
		.addStep(LoadStoreOptimization::storeAndLoad,
			(match, graph, block) -> replace(match.load(), match.load().getMem(), match.value()))
		.addStep(LoadStoreOptimization::loadAndLoad, LoadStoreOptimization::replaceLoadAndLoad)
		.build();

	// TODO: BIG FAT TODO
	private static final List<SwapRule<?, ?>> SWAP_RULES = List.of(
		// Load can be moved above Mod/Div if Ptr does not depend on Mod/Div
		new SwapRule<>(Div.class, Load.class, (div, load) -> isNotResultOf(load.getPtr(), div)),
		new SwapRule<>(Mod.class, Load.class, (mod, load) -> isNotResultOf(load.getPtr(), mod)),
		// Store can be moved below Div/Mod
		new SwapRule<>(Store.class, Div.class, (store, div) -> true),
		new SwapRule<>(Store.class, Mod.class, (store, mod) -> true)
		// Loads can be reordered if the result of the upper is not used for the lower.
		// In that case we move them in one direction for LoadAndLoadPattern
	/*	new SwapRule<>(Load.class, Load.class, (upper, lower) -> isNotResultOf()
			&& upper.getPtr().getNr() > lower.getPtr().getNr())*/
		// without alias analysis, this does not work for Stores, as we need to keep the last Store at
		// its position in a Store-chain. However, if we can guarantee that the Stores come from the same
		// instance and their index in the instance is known to be different, we can swap them too
		// => TODO
	);

	private static boolean isNotResultOf(Node value, Node node) {
		return !(value instanceof Proj proj && proj.getPred().equals(node));
	}

	public LoadStoreOptimization(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> loadStoreOptimizations() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("LoadStoreOptimization")
			.withOptimizationFunction(graph -> {
				int runs = 0;
				while (true) {
					BackEdges.enable(graph);
					LoadStoreOptimization loadStoreOptimization = new LoadStoreOptimization(graph);
					loadStoreOptimization.optimize();
					BackEdges.disable(graph);
					if (!loadStoreOptimization.changed) {
						break;
					} else if (LOGGER.isDebugEnabled()) {
						dumpGraph(graph, "load-store-iteration");
					}
					runs++;
				}
				boolean changed = runs > 0;
				if (changed) {
					dumpGraph(graph, "load-store");
				}
				return changed;
			})
			.build();
	}

	private static boolean replaceLoadAndLoad(LoadAndLoadPattern.Match match, Graph graph, Node block) {
		// this is a bit ugly, but we need to get the data proj from load0 first
		for (BackEdges.Edge load0Edge : BackEdges.getOuts(match.load0())) {
			if (!load0Edge.node.getMode().equals(Mode.getM())) {
				Node load0Proj = load0Edge.node; // here it is
				// now we need to get the data proj from load1
				for (BackEdges.Edge load1Edge : BackEdges.getOuts(match.load1())) {
					if (!load1Edge.node.getMode().equals(Mode.getM())) {
						// then, rewire each of its users to the load0 data proj
						for (BackEdges.Edge userEdge : BackEdges.getOuts(load1Edge.node)) {
							userEdge.node.setPred(userEdge.pos, load0Proj);
						}
					} else {
						for (BackEdges.Edge memoryUserEdge : BackEdges.getOuts(load1Edge.node)) {
							memoryUserEdge.node.setPred(memoryUserEdge.pos, match.load1().getMem());
						}
					}
				}
				return true;
			}
		}
		return false;
	}

	private void optimize() {
		while (swapNodes()) {
			changed = true;
		}
		LOGGER.debug("Swapped: %s", changed);
		graph.walk(this);
	}

	private boolean swapNodes() {
		Set<Phi> visitedPhis = new HashSet<>();
		boolean changed = false;
		for (Node endBlockPred : graph.getEndBlock().getPreds()) {
			if (!(endBlockPred instanceof Return ret)) {
				continue;
			}
			changed |= walkUp(ret.getMem(), visitedPhis, Optional.empty());
		}
		return changed;
	}

	private boolean walkUp(Node node, Set<Phi> visitedPhis, Optional<Node> memorySuccessor) {
		if (node instanceof Phi phi) {
			if (!visitedPhis.add(phi)) {
				return false;
			} else {
				for (Node pred : phi.getPreds()) {
					walkUp(pred, visitedPhis, Optional.empty());
				}
			}
		}
		if (node instanceof Proj proj) {
			return walkUp(proj.getPred(), visitedPhis, memorySuccessor);
		}
		if (memorySuccessor.isPresent()) {
			Node successor = memorySuccessor.get();
			for (SwapRule<?, ?> rule : SWAP_RULES) {
				if (typedSwap(rule, node, successor)) {
					return true;
				}
			}
		}
		if (node.getPredCount() == 0) {
			return false;
		}
		return walkUp(node.getPred(0), visitedPhis, Optional.of(node));
	}

	@SuppressWarnings("unchecked")
	private static <A extends Node, B extends Node> boolean typedSwap(SwapRule<A, B> rule, Node upper, Node lower) {
		return rule.swap((A) upper, (B) lower); // TODO better way than ugly cast?
	}

	@Override
	public void defaultVisit(Node n) {
		changed |= OPTIMIZATIONS.optimize(n, n.getGraph(), n.getBlock());
	}

	private static boolean replace(Node node, Node previousMemory, Node replacement) {
		Util.replace(node, previousMemory, replacement);
		return true;
	}

	private record SwapRule<A extends Node, B extends Node>(
		Class<A> upper,
		Class<B> lower,
		SwapCheck<A, B> check
	) {
		boolean swap(A upper, B lower) {
			if (upper().isInstance(upper) && lower().isInstance(lower) && check().canSwap(upper, lower)) {
				// get mem predecessors
				Node upperPred = upper.getPred(0);
				Node lowerPred = lower.getPred(0);
				// swap mem predecessors
				upper.setPred(0, lowerPred);
				lower.setPred(0, upperPred);

				// get mem successors
				BackEdges.Edge upperSuccessor = memorySuccessor(upper);
				BackEdges.Edge lowerSuccessor = memorySuccessor(lower);
				// swap mem successors
				upperSuccessor.node.setPred(upperSuccessor.pos, lower);
				lowerSuccessor.node.setPred(lowerSuccessor.pos, upper);
				return true;
			}
			return false;
		}
	}

	private static BackEdges.Edge memorySuccessor(Node node) {
		for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
			if (edge.node.getMode().equals(Mode.getM())) {
				return edge;
			}
		}
		throw new InternalCompilerException("Memory out vanished for " + node);
	}

	@FunctionalInterface
	private interface SwapCheck<A extends Node, B extends Node> {
		boolean canSwap(A a, B b);
	}

	@FiAscii("""
		┌───────────┐       ┌─────────────┐
		│ mem: Proj │       │ address: *  │
		└───┬───────┘       └───┬───────┬─┘
		    │                   │       │
		    │   ┌───────────────┘       │
		    │   │                       │
		    │   │          ┌──────────┐ │
		    │   │    ┌─────┤ value: * │ │
		    │   │    │     └──────────┘ │
		┌───▼───▼────▼─┐                │
		│ store: Store │                │
		└───┬──────────┘                │
		    │                           │
		    │                           │
		    │                           │
		 ┌──▼─────────┐                 │
		 │ proj: Proj │                 │
		 └──┬─────────┘                 │
		    │                           │
		    │       ┌───────────────────┘
		    │       │
		 ┌──▼───────▼─┐
		 │ load: Load │
		 └────────────┘
		""")
	public static Optional<StoreAndLoadPattern.Match> storeAndLoad(Node node) {
		return StoreAndLoadPattern.match(node);
	}

	@FiAscii("""
		┌───────────┐  ┌─────────────┐
		│ mem: Proj │  │ address: *  │
		└──┬────────┘  └───┬──────┬──┘
		   │               │      │
		   │   ┌───────────┘      │
		   │   │                  │
		   │   │                  │
		   │   │                  │
		   │   │                  │
		┌──▼───▼──────┐           │
		│ load0: Load │           │
		└──┬──────────┘           │
		   │                      │
		   │                      │
		   │                      │
		┌──▼─────────┐            │
		│ proj: Proj │            │
		└──┬─────────┘            │
		   │                      │
		   │       ┌──────────────┘
		   │       │
		┌──▼───────▼──┐
		│ load1: Load │
		└─────────────┘""")
	public static Optional<LoadAndLoadPattern.Match> loadAndLoad(Node node) {
		return LoadAndLoadPattern.match(node);
	}

	@FiAscii("""
		┌───────────┐        ┌─────────────┐
		│ mem: Proj │        │ address: *  │
		└───┬───────┘        └───┬───────┬─┘
		    │                    │       │
		    │   ┌────────────────┘       │
		    │   │                        │
		    │   │           ┌──────────┐ │
		    │   │     ┌─────┤ val0: *  │ │
		    │   │     │     └──────────┘ │
		┌───▼───▼─────▼─┐                │
		│ store0: Store │                │
		└───┬───────────┘                │
		    │                            │
		    │                            │
		    │                            │
		 ┌──▼─────────┐                  │
		 │ proj: Proj │                  │
		 └──┬─────────┘                  │
		    │                            │
		    │   ┌────────────────────────┘
		    │   │
		    │   │          ┌──────────┐
		    │   │     ┌────┤ val1: *  │
		    │   │     │    └──────────┘
		┌───▼───▼─────▼─┐
		│ store1: Store │
		└───────────────┘
		""")
	public static Optional<StoreAndStorePattern.Match> storeAndStore(Node node) {
		return StoreAndStorePattern.match(node);
	}
}
