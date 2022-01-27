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

	public static native double get_block_execfreq(Pointer ptr);

	public static native void ir_estimate_execfreq(Pointer ptr);

}
