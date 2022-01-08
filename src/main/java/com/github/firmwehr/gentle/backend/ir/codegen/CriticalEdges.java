package com.github.firmwehr.gentle.backend.ir.codegen;

import com.github.firmwehr.gentle.firm.Util;
import firm.Graph;
import firm.nodes.Block;
import firm.nodes.Cond;
import firm.nodes.IJmp;
import firm.nodes.Node;
import firm.nodes.Proj;
import firm.nodes.Switch;

import java.util.List;

public class CriticalEdges {

	public static void breakCriticalEdges(Graph graph) {
		graph.walkBlocksPostorder(block -> {
			List<Node> preds = Util.predsStream(block).toList();
			for (int i = 0; i < preds.size(); i++) {
				Node pred = skipProjs(preds.get(i));

				// Fragile ops (Load, Store, Div, Mod, ASM) might have multiple control flow outputs (exceptional
				// completion or regular). These should be handled, but we do not handle exceptions anyway.
				if (isForking(pred)) {
					Block newBlock = (Block) block.getGraph().newBlock(new Node[]{preds.get(i)});
					block.setPred(i, block.getGraph().newJmp(newBlock));
				}
			}
		});
	}

	private static Node skipProjs(Node node) {
		if (node instanceof Proj proj) {
			return skipProjs(proj.getPred());
		}
		return node;
	}

	private static boolean isForking(Node node) {
		return node instanceof IJmp || node instanceof Cond || node instanceof Switch;
	}
}
