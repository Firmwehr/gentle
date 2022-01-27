package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.firm.GentleBindings;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.ArrayListMultimap;
import firm.Graph;
import firm.bindings.binding_irdom;
import firm.bindings.binding_irgopt;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.github.firmwehr.gentle.util.GraphDumper.dumpGraph;

public class GlobalValueNumbering extends NodeVisitor.Default {

	private static final Logger LOGGER = new Logger(GlobalValueNumbering.class);

	private final Graph graph;

	public GlobalValueNumbering(Graph graph) {
		this.graph = graph;
	}

	public static GraphOptimizationStep<Graph, Boolean> deduplicate() {
		return GraphOptimizationStep.<Graph, Boolean>builder()
			.withDescription("GlobalValueNumbering")
			.withOptimizationFunction(graph -> {

				// prior optimizations might have messed up dom information, we need to recalculate it
				binding_irdom.compute_doms(graph.ptr);

				boolean modifiedGraph = false;
				while (true) {
					GlobalValueNumbering globalValueNumbering = new GlobalValueNumbering(graph);
					boolean changed = globalValueNumbering.applyGlobalValueNumbering();

					if (changed) {
						modifiedGraph = true;
						dumpGraph(graph, "gvn");
					} else {
						break;
					}
				}
				return modifiedGraph;
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

			// check if firm considers them equal
			if (node.equals(thatNode)) {
				return true;
			}

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
					"encountered offset node (do we even have these?)");
				case iro_Sel -> throw new InternalCompilerException("encountered sel node (should have been lowered)");
				case iro_Size -> throw new InternalCompilerException("encountered size node (do we even have these?)");
				case iro_Store -> {
					var n0 = (Store) node;
					var n1 = (Store) thatNode;
					if (!n0.getType().equals(n1.getType())) {
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
				case iro_Switch -> throw new InternalCompilerException("switch node is not supported");
				case iro_Jmp, iro_Start, iro_Cond, iro_Phi -> {
					/* some nodes should never be equal, unless firm itself considers them equal, but this way already
					 * check a few lines above. if we reached this place, they are simply considered not equal
					 */
					return false;
				}
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
	 * <p>
	 * For phi nodes, the rules are more complex. They don't follow dominance restrictions and are able to reference
	 * blocks outside of the current dominator tree path. Therefore, the above scheme may eliminate nodes that are
	 * still
	 * referenced by a phi node without us knowing. To solve this issue, we keep track of every replacement we did and
	 * rewire all phi nodes at the end.
	 */
	private static class GlobalValueNumberingWalker {

		/**
		 * Keeps track of surviving phi nodes.
		 */
		private final Set<Phi> phis = new HashSet<>();

		/**
		 * Maps replaced notes to their replacement.
		 */
		private final Map<Node, Node> replacements = new HashMap<>();

		/**
		 * Precomputed (and updated) for accessing nodes of blocks without complex graph traversal mid-exe.cution
		 */
		private final ArrayListMultimap<Block, Node> blocks = ArrayListMultimap.create();

		/**
		 * Tracks current available expressions for current subtree.
		 */
		private final Map<NodeHashKey, Node> availableExpressions = new HashMap<>();

		private final Graph graph;
		private final Set<Block> openBlocks = new HashSet<>();

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

			// during traversal, some replacements may haven been replaced themself, we need to resolve these chains
			for (var start : new ArrayList<>(replacements.keySet())) {
				squashChain(replacements, start);
			}

			// rewire all phi nodes that are abount to loose their pred
			for (var phi : phis) {

				// can't use rewire method, since we are not performing equality checks at this point
				// since we have no block information anymore
				int predCount = phi.getPredCount();
				for (int i = 0; i < predCount; i++) {
					var pred = phi.getPred(i);

					var replacement = replacements.get(pred);
					if (replacement != null) {
						phi.setPred(i, replacement);
						hasModifiedGraph = true;
					}
				}
			}

			// remove killed nodes (don't worry, they aren't sentient)
			for (var node : replacements.keySet()) {
				LOGGER.debug("killing %s", node);
				Graph.killNode(node);
			}
			binding_irgopt.remove_bads(graph.ptr);
		}

		/**
		 * Recursive magic in replacment map. Traverse and squash chains of replacements, until each replacement points
		 * to the final surviving node.
		 *
		 * @param replacement The replacement map to be squashed.
		 * @param start Start node of chain to be squashed.
		 *
		 * @return The true replacment target of the given start node.
		 */
		private static <T> T squashChain(Map<T, T> replacement, T start) {
			var target = replacement.get(start);

			// check if current target has itself a target, which would require squashing
			if (replacement.containsKey(target)) {
				// squash chain and return true terminating target
				var trueTarget = squashChain(replacement, target);
				replacement.put(start, trueTarget);
				return trueTarget;
			} else {
				// target has no successors, is true target
				return target;
			}
		}

		/**
		 * Called when DFS enters block.
		 *
		 * @param block The block we just entered.
		 */
		private void onEnter(Block block) {
			openBlocks.add(block);

			var nodes = blocks.get(block);

			// stabilize local block
			var lastAvailable = new HashMap<>(availableExpressions);
			var currentAvailable = new HashMap<NodeHashKey, Node>();

			boolean runAgain = true;
			boolean firstRun = true;
			while (runAgain) {
				runAgain = false;

				// if we clear it at the end of the loop, we can't carry over available nodes
				currentAvailable.clear();

				var replaced = new ArrayList<Node>();
				for (var node : nodes) {

					// if we have been replaced, we don't need to reroute inputs and don't carry over
					var replacement = lastAvailable.get(new NodeHashKey(node));
					if (replacement != null && !node.equals(replacement)) {
						replacements.put(node, replacement);
						replaced.add(node);
						continue;
					}

					// check if we need to rewire outgoing edges
					if (rewireNode(lastAvailable, node)) {
						runAgain = true;
						hasModifiedGraph = true;
					}

					// rewire may create identical node with already available expression, but we deal with this later
					currentAvailable.put(new NodeHashKey(node), node);
				}

				// remove nodes from block if they have been replaced and should no longer be considere
				nodes.removeAll(replaced);

				// rewire may have created duplicates with available expression, so we always override them
				lastAvailable = new HashMap<>(currentAvailable);
				lastAvailable.putAll(availableExpressions);

				// dump graphs if we had actual changes (next statement will destroy this information)
				if (runAgain) {
					dumpGraph(graph, "gvn-iter-block" + block.getNr());
				}

				// first run does NOT have access to full block local node identities, so we always need a second run
				if (firstRun) {
					runAgain = true;
					firstRun = false;
				}
			}

			// extract surviving phi nodes, we might need to update them later
			for (var node : currentAvailable.values()) {
				if (node instanceof Phi phi) {
					phis.add(phi);
				}
			}

			// local block stabilized, update available expression
			availableExpressions.putAll(currentAvailable);
		}

		private boolean rewireNode(HashMap<NodeHashKey, Node> lastAvailable, Node node) {
			boolean runAgain = false;
			boolean isPhi = node instanceof Phi;
			var predCount = node.getPredCount();
			for (int i = 0; i < predCount; i++) {
				var pred = node.getPred(i);

				// phis can access and find identities across non dominated blocks, which would rewire wrong nodes
				// to prevent that, make sure we only touch preds that are currently accessible
				if (isPhi) {
					var predBlock = (Block) pred.getBlock();
					if (!openBlocks.contains(predBlock)) {
						continue;
					}
				}

				var existing = lastAvailable.get(new NodeHashKey(pred));

				// check if edge needs rewiring
				if (existing != null && !pred.equals(existing)) {
					LOGGER.debug("rewire edge %s of %s from %s to %s in graph %s", i, node, pred, existing, graph);

					node.setPred(i, existing);
					runAgain = true;
				}
			}
			return runAgain;
		}

		/**
		 * Called when DFS leaves block.
		 *
		 * @param block The block we just left.
		 */
		private void onExit(Block block) {
			openBlocks.remove(block);

			// remove all expressions from current block from available expressions
			var nodes = blocks.get(block);
			for (Node node : nodes) {
				var hash = new NodeHashKey(node);

				// every expression is unique to first block it appeared (duplicates have been eradicated on enter)
				availableExpressions.remove(hash);
			}
		}
	}
}
