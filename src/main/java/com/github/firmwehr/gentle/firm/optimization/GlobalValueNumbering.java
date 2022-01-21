package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.ArrayListMultimap;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class GlobalValueNumbering extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(GlobalValueNumbering.class, Logger.LogLevel.DEBUG);

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

				GlobalValueNumbering globalValueNumbering = new GlobalValueNumbering(graph);
				boolean changed = globalValueNumbering.applyGlobalValueNumbering();
				if (changed) {
					dumpGraph(graph, "gvn");
				}
				return changed;
			})
			.build();
	}

	private boolean applyGlobalValueNumbering() {
		var walker = new GlobalValueNumbering.GlobalValueNumberingWalker(graph);
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
				case iro_Jmp -> {
					// jumps can not be merged (really bad things will happen, if you try it)
					return node.equals(thatNode);
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


	}

	/**
	 * This class uses the Firm backend to perform a depth first search in the dominator tree. During the search, we
	 * keep a list of available expressions (nodes) and each time we enter a new block, we check if we have duplicated
	 * expressions. On exit, we remove the available expressions from the available expression list. This ensures that
	 * the available expression list will only contain such expressions that are currently available to the current
	 * block.
	 */
	private static class GlobalValueNumberingWalker {

		// firm can't enumerate all nodes in block without shitting itself
		private final ArrayListMultimap<Block, Node> blocks = ArrayListMultimap.create();
		private final HashMap<NodeHashKey, Node> availableExpressions = new HashMap<>();
		private final Set<Node> emergencyIntelligenceIncinerator = new HashSet<>();

		private final Graph graph;

		private boolean hasModifiedGraph;

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

			// remove killed nodes (don't worry, they aren't sentient)
			for (var node : emergencyIntelligenceIncinerator) {
				LOGGER.debug("killing %s", node);
				Graph.killNode(node);
			}
			firm.bindings.binding_irgopt.remove_bads(graph.ptr);
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

				// if we clear it at the end of the loop, we can't carry over available nodes
				currentAvailable.clear();

				var replaced = new HashSet<Node>();
				for (var node : nodes) {

					// if we have been replaced, we don't need to reroute inputs and don't carry over
					var replacement = lastAvailable.get(new NodeHashKey(node));
					if (replacement != null && !node.equals(replacement)) {
						replaced.add(node);
						continue;
					}

					// check if we need to rewire outgoing edges
					var rewired = false;
					var predCount = node.getPredCount();
					for (int i = 0; i < predCount; i++) {
						var pred = node.getPred(i);

						var existing = lastAvailable.get(new NodeHashKey(pred));

						// check if edge needs rewiring
						if (existing != null && !pred.equals(existing)) {
							LOGGER.debug("rewire edge %s of %s from %s to %s in graph %s", i, node, pred, existing,
								graph);

							node.setPred(i, existing);
							hasModifiedGraph = true;
							runAgain = true;
							rewired = true;
						}
					}

					// rewire may create identical node with already available expression, but we deal with this later
					currentAvailable.put(new NodeHashKey(node), node);
				}

				// remove nodes from block if they have been replaced and should no longer be considere
				nodes.removeAll(replaced);
				emergencyIntelligenceIncinerator.addAll(replaced);

				// rewire may have created duplicates with available expression, so we always override them
				lastAvailable = new HashMap<>(currentAvailable);
				lastAvailable.putAll(availableExpressions);

				// dump graphs if we had actual changes (next statement will destroy this information)
				if (runAgain) {
					dumpGraph(block.getGraph(), "gvn-iter");
				}

				// first run does have access to full block local node identities, so we always need a second run
				if (firstRun) {
					runAgain = true;
					firstRun = false;
				}
			}

			// local block stabilized, move on to next block
			availableExpressions.putAll(currentAvailable);

			/* following blocks may have dependencies on nodes that were replaced in current block
			 * in order to properly redirect those,
			 * */
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
