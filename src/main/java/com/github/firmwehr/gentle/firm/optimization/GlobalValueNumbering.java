package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.ArrayListMultimap;
import firm.BackEdges;
import firm.Graph;
import firm.nodes.Address;
import firm.nodes.Block;
import firm.nodes.Cmp;
import firm.nodes.Const;
import firm.nodes.Div;
import firm.nodes.Load;
import firm.nodes.Mod;
import firm.nodes.Node;
import firm.nodes.NodeVisitor;
import firm.nodes.Phi;
import firm.nodes.Proj;
import firm.nodes.Store;

import java.util.ArrayList;
import java.util.HashMap;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class GlobalValueNumbering extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(GlobalValueNumbering.class);

	private final Graph graph;

	// poor mans union find structure
	private ArrayListMultimap<NodeHashKey, Node> lastUnions = ArrayListMultimap.create();
	private ArrayListMultimap<NodeHashKey, Node> currentUnions = ArrayListMultimap.create();

	private boolean hasChanged;

	private boolean consecutiveRun;

	public GlobalValueNumbering(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> deduplicate() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("GlobalValueNumbering")
			.withOptimizationFunction(graph -> {

				// prior optimizations might have messed up dom information, we need to recalculate it
				firm.bindings.binding_irdom.compute_doms(graph.ptr);

				// node replacement and efficient change propagation requires back edges
				BackEdges.enable(graph);
				GlobalValueNumbering globalValueNumbering = new GlobalValueNumbering(graph);

				boolean changed = globalValueNumbering.applyGlobalValueNumbering();
				if (changed) {
					dumpGraph(graph, "gvn");
				}

				BackEdges.disable(graph);
				return changed;
			})
			.build();
	}

	private boolean applyGlobalValueNumbering() {
		var walker = new GlobalValueNumbering.NodeHashKey.GlobalValueNumberingWalker(graph);
		walker.walkTheWalk();
		return walker.hasModifiedGraph();
	}

	/**
	 * Wrapper around Node, so we can implement our own hash code.
	 * <p>
	 * Nodes are considered equal if they share the same predecessors and are configured the same way.
	 */
	private record NodeHashKey(Node node) {

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			NodeHashKey that = (NodeHashKey) o;
			var thatNode = that.node;

			// check node op
			if (node.getOpCode() != thatNode.getOpCode()) {
				return false;
			}

			// check pred node count
			if (node.getPredCount() != thatNode.getPredCount()) {
				return false;
			}

			// check each pred
			for (int i = 0; i < node.getPredCount(); i++) {
				if (!node.getPred(i).equals(thatNode.getPred(i))) {
					return false;
				}
			}

			// actuall different objects, must use equals
			if (!node.getMode().equals(thatNode.getMode())) {
				return false;
			}

			// some nodes can be configured (like const) this is a special case for each node
			switch (node.getOpCode()) {
				case iro_Address -> {
					var n0 = (Address) node;
					var n1 = (Address) thatNode;
					if (!n0.getEntity().equals(n1.getEntity())) {
						return false;
					}
				}
				case iro_Cmp -> {
					var n0 = (Cmp) node;
					var n1 = (Cmp) thatNode;
					if (!n0.getRelation().equals(n1.getRelation())) {
						return false;
					}
				}
				case iro_Const -> {
					var n0 = (Const) node;
					var n1 = (Const) thatNode;
					// TODO: does tarval do internal deduplication?
					if (n0.getTarval().asLong() != n1.getTarval().asLong()) {
						return false;
					}
				}
				case iro_Div -> {
					var n0 = (Div) node;
					var n1 = (Div) thatNode;
					if (!n0.getResmode().equals(n1.getResmode()) || n0.getNoRemainder() != n1.getNoRemainder()) {
						return false;
					}
				}
				case iro_Id -> throw new InternalCompilerException(
					"encountered id node (should have been replaced by someone?)");
				case iro_Load -> {
					var n0 = (Load) node;
					var n1 = (Load) thatNode;
					if (!n0.getMode().equals(n1.getMode()) || !n0.getType().equals(n1.getType())) {
						return false;
					}
				}
				case iro_Member -> throw new InternalCompilerException(
					"encountered member node (should have been lowered)");
				case iro_Mod -> {
					var n0 = (Mod) node;
					var n1 = (Mod) thatNode;
					if (!n0.getResmode().equals(n1.getResmode())) {
						return false;
					}
				}
				case iro_Offset -> throw new InternalCompilerException(
					"encountered offset node (do we haven have these?)");
				case iro_Phi -> {
					var n0 = (Phi) node;
					var n1 = (Phi) thatNode;
					if (n0.getLoop() != n1.getLoop()) {
						return false;
					}
				}
				case iro_Proj -> {
					var n0 = (Proj) node;
					var n1 = (Proj) thatNode;
					if (n0.getNum() != n1.getNum()) {
						return false;
					}
				}
				case iro_Sel -> throw new InternalCompilerException("encountered sel node (should have been lowered)");
				case iro_Size -> throw new InternalCompilerException(
					"encountered size node (do we haven have " + "these?)");
				case iro_Start -> {
					// there should only ever be one start node, but at the same time, it should never need to be
					// compared
					return false;
				}
				case iro_Store -> {
					var n0 = (Store) node;
					var n1 = (Store) thatNode;
					if (!n0.getType().equals(n1.getType())) {
						return false;
					}
				}
				case iro_Switch -> throw new InternalCompilerException("switch node is not supported");
				default -> {/*no op*/}
			}

			return true;
		}

		@Override
		public int hashCode() {
			// nodes are considered equal if they themself are equally configured and share the same preds
			// this hash ignores node configuration, but that's okay
			int hash = node.getClass().hashCode();
			for (Node pred : node.getPreds()) {
				hash ^= pred.ptr.hashCode();
			}

			return hash;
		}

		/**
		 * This class uses the Firm backend to perform a depth first search in the dominator tree. During the
		 * search, we
		 * keep a list of available expressions (nodes) and each time we enter a new block, we check if we have
		 * duplicated expressions. On exit, we remove the available expressions from the available expression list.
		 * This
		 * ensures that the available expression list will only contain such expressions that are currently
		 * available to
		 * the current block.
		 */
		private static class GlobalValueNumberingWalker {

			// firm can't enumerate all nodes in block without shitting itself
			private final ArrayListMultimap<Block, Node> blocks = ArrayListMultimap.create();
			private final HashMap<NodeHashKey, Node> availableExpressions = new HashMap<>();

			private final Graph graph;

			private boolean hasModifiedGraph = false;

			public GlobalValueNumberingWalker(Graph graph) {
				this.graph = graph;
			}

			public boolean hasModifiedGraph() {
				return hasModifiedGraph;
			}

			public void walkTheWalk() {
				graph.walkTopological(new Default() {
					@Override
					public void defaultVisit(Node n) {

						Block block = (Block) n.getBlock();
						if (block != null) {
							blocks.put(block, n);
						}
					}
				});

				GentleBindings.walkDominatorTree(graph, this::onEnter, this::onExit);
			}

			/**
			 * Called when DFS enters block.
			 *
			 * @param block The block we just entered.
			 */
			private void onEnter(Block block) {

				var nodes = blocks.get(block);

				// stabalize local block
				var lastAvailable = new HashMap<>(availableExpressions);
				var currentAvailable = new HashMap<NodeHashKey, Node>();

				boolean runAgain = true;
				boolean firstRun = true;
				while (runAgain) {
					runAgain = false;

					var it = nodes.iterator();
					while (it.hasNext()) {
						var node = it.next();
						var hash = new NodeHashKey(node);

						var existing = lastAvailable.get(hash);
						if (existing != null && !existing.equals(node)) {

							// capture depender and remove their hash from expression, since they are about to change
							var dependers = new ArrayList<Node>();
							for (BackEdges.Edge edge : BackEdges.getOuts(node)) {
								var depender = edge.node;
								if (!depender.getBlock().equals(block)) {
									// node is part of LATER block, so we don't consider it here
									continue;
								}

								var dependerHash = new NodeHashKey(depender);
								currentAvailable.remove(dependerHash);
								dependers.add(depender);
							}

							// exchange node (which also means it will be removed from block)
							// TODO: exchange might leave Id node (shouldnt, since we have backedges enabled)
							LOGGER.debug("replacing %s with %s in graph %s", node, existing, graph);
							Graph.exchange(node, existing);
							runAgain = true;
							hasModifiedGraph = true;
							it.remove();

							// depender need to be updated
							for (Node depender : dependers) {
								var dependerHash = new NodeHashKey(depender);
								currentAvailable.putIfAbsent(dependerHash, depender);
							}

						} else {
							// node hasn't been replaced, take over to next iteration
							currentAvailable.put(hash, node);
						}

					}
					lastAvailable = currentAvailable;
					lastAvailable.putAll(availableExpressions);
					currentAvailable = new HashMap<>();

					// dump graphs if we had actual changes (next statement will destroy this information)
					if (runAgain) {
						dumpGraph(block.getGraph(), "gvn");
					}

					// first run does have access to full block local node identities, so we always need a second run
					if (firstRun) {
						runAgain = true;
						firstRun = false;
					}
				}

				// local block stabilized, move on to next block
				availableExpressions.putAll(currentAvailable);
			}

			/**
			 * Called when DFS leaves block.
			 *
			 * @param block The block we just left.
			 */
			private void onExit(Block block) {
				// remove all expressions from current block from available expressions
				var nodes = blocks.get(block);
				for (Node node : nodes) {
					var hash = new NodeHashKey(node);

					// every expression is unqie to first block it appeared (duplicates have been eradicated on enter)
					availableExpressions.remove(hash);
				}
			}
		}
	}
}
