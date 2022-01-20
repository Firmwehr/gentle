package com.github.firmwehr.gentle.firm.optimization;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.output.Logger;
import com.google.common.collect.ArrayListMultimap;
import firm.Graph;
import firm.nodes.Address;
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
				int runs = 0;

				// gvn keeps state of laster iteration
				GlobalValueNumbering globalValueNumbering = new GlobalValueNumbering(graph);
				while (true) {

					globalValueNumbering.applyGlobalValueNumbering();

					if (!globalValueNumbering.hasChanged) {
						break;
					} else if (LOGGER.isDebugEnabled() && runs > 1) {
						dumpGraph(graph, "gvn-iteration");
					}
					runs++;
				}

				// first run will never change graph and is only for collecting data
				boolean changed = runs > 1;
				if (changed) {
					dumpGraph(graph, "gvn");
				}
				return changed;
			})
			.build();
	}

	private void applyGlobalValueNumbering() {
		hasChanged = false;

		// TODO: optimize only walk over updated nodes after first iteration (can use lastUnions? (and skip index 0?))
		// TODO probably requires back edges...
		graph.walk(this);

		// first run is only collecting data, so we always assume something can be changed
		if (!consecutiveRun) {
			hasChanged = true;
			consecutiveRun = true;
		}

		for (var union : currentUnions.asMap().values()) {
			if (union.size() > 1) {
				LOGGER.debug("common nodes: %s", union);
			}
		}

		// advance unions to next run
		lastUnions = currentUnions;
		currentUnions = ArrayListMultimap.create();
	}

	@Override
	public void defaultVisit(Node n) {

		/* big brain play of the century: we can rewire edges while we calculate the next iteration
		 *
		 * this only works if we compute the new hash after we have rewired the node
		 */

		// 1. rewire preds if common sub node has been found last time
		var count = n.getPredCount();
		for (int i = 0; i < count; i++) {

			// get union leader for current target (might be same, if already linked to leader)
			var old = n.getPred(i);
			var oldHash = new NodeHashKey(old);

			// rewire edge if not currently linked with union leader
			var list = lastUnions.get(oldHash);
			if (!list.isEmpty()) {
				var next = list.get(0);
				if (!old.equals(next) && old.getBlock().equals(next.getBlock())) {
					hasChanged = true;
					n.setPred(i, next);
				}
			}
		}

		// 2. calculate new hash (which will reflect new updated preds)
		var newHash = new NodeHashKey(n);
		currentUnions.put(newHash, n);
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
	}

}
