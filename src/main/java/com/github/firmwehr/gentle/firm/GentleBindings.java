package com.github.firmwehr.gentle.firm;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import firm.BlockWalker;
import firm.Firm;
import firm.Graph;
import firm.nodes.Block;
import firm.nodes.Node;

public enum GentleBindings {
	;

	static {
		Native.register(Firm.VERSION.getFileName());
	}

	public static native void dom_tree_walk_irg(
		Pointer graph, DominatorWalkCallback pre, DominatorWalkCallback post, Pointer env
	);

	@SuppressWarnings("unused") // that's jna bby
	public record DominatorWalkCallback(BlockWalker walker) implements Callback {

		public void callback(Pointer nodePtr, Pointer unused) {
			var node = Node.createWrapper(nodePtr);
			walker.visitBlock((Block) node);
		}
	}

	public static void walkDominatorTree(Graph graph, BlockWalker preWalker, BlockWalker postWalker) {
		dom_tree_walk_irg(graph.ptr, new DominatorWalkCallback(preWalker), new DominatorWalkCallback(postWalker),
			null);
	}

	/**
	 * Returns the root loop info (if exists) for an irg.
	 *
	 * @param irg the pointer to the firm graph
	 */
	public static native Pointer get_irg_loop(Pointer irg);

	/**
	 * Returns the loop n is contained in.  NULL if node is in no loop.
	 *
	 * @param irn the pointer to the firm node
	 */
	public static native Pointer get_irn_loop(Pointer irn);

	/**
	 * Returns outer loop, itself if outermost.
	 *
	 * @param loop the loop pointer
	 */
	public static native Pointer get_loop_outer_loop(Pointer loop);

	/**
	 * Returns nesting depth of this loop
	 *
	 * @param loop the loop pointer
	 */
	public static native int get_loop_depth(Pointer loop);

	/**
	 * Returns the number of elements contained in loop.
	 *
	 * @param loop the loop pointer
	 */
	public static native int get_loop_n_elements(Pointer loop);

	/**
	 * Returns a loop element. A loop element can be interpreted as a kind pointer, an ir_node* or an ir_loop*.
	 *
	 * @param loop the loop pointer
	 * @param pos the position, needs to be {@code 0 <= pos <= } {@link #get_loop_n_elements(Pointer)}
	 */
	public static native Pointer get_loop_element(Pointer loop, int pos);

	/**
	 * Computes Intra-procedural control flow loop tree on demand.
	 *
	 * @param irg the graph pointer
	 */
	public static native void assure_loopinfo(Pointer irg);

	public static native double get_block_execfreq(Pointer ptr);

	public static native void ir_estimate_execfreq(Pointer ptr);

}
