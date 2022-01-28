package com.github.firmwehr.gentle.backend.ir.register;

import com.github.firmwehr.gentle.backend.ir.IkeaBløck;
import com.github.firmwehr.gentle.backend.ir.IkeaParentBløck;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaConst;
import com.github.firmwehr.gentle.backend.ir.nodes.IkeaNode;
import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Interference {

	private final Uses uses;
	private final Dominance dominance;
	private final LifetimeAnalysis liveliness;
	private final ControlFlowGraph controlFlowGraph;

	public Interference(
		Uses uses, Dominance dominance, LifetimeAnalysis liveliness, ControlFlowGraph controlFlowGraph
	) {
		this.uses = uses;
		this.dominance = dominance;
		this.liveliness = liveliness;
		this.controlFlowGraph = controlFlowGraph;
	}

	public boolean doInterfere(IkeaNode first, IkeaNode second) {
		IkeaNode t;
		IkeaNode b;
		if (dominance.dominates(first, second)) {
			t = first;
			b = second;
		} else {
			t = second;
			b = first;
		}

		if (liveliness.getLiveOut(b.block()).contains(t)) {
			return true;
		}

		for (IkeaNode use : uses.uses(t)) {
			if (dominance.dominates(b, use)) {
				return true;
			}
		}

		return false;
	}

	public /* N */ Set<IkeaNode> interferenceNeighbours(IkeaNode x) {
		Set<IkeaNode> N = new HashSet<>();
		Set<IkeaBløck> visited = new HashSet<>();

		for (IkeaNode use : uses.uses(x)) {
			findInt(use.block(), x, N, visited);
		}

		return N.stream()
			.filter(it -> !(it instanceof IkeaConst) && !it.registerIgnore())
			.filter(it -> !it.equals(x))
			.collect(Collectors.toSet());
	}

	private void findInt(IkeaBløck block, IkeaNode x, Set<IkeaNode> N, Set<IkeaBløck> visited) {
		visited.add(block);
		Set<IkeaNode> liveouts = new HashSet<>(liveliness.getLiveOut(block));

		for (IkeaNode node : Lists.reverse(block.nodes())) {
			if (node.equals(x)) {
				break;
			}
			liveouts.remove(node);
			liveouts.addAll(node.inputs());

			if (liveouts.contains(x)) {
				N.addAll(liveouts);
			}
		}

		for (IkeaParentBløck parent : block.parents()) {
			IkeaBløck parentBlock = parent.parent();
			IkeaNode lastInParent = parentBlock.nodes().get(parentBlock.nodes().size() - 1);
			if (dominance.dominates(x, lastInParent) && !visited.contains(parentBlock)) {
				findInt(parentBlock, x, N, visited);
			}
		}
	}
}
