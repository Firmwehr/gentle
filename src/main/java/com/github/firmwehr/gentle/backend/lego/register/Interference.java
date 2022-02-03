package com.github.firmwehr.gentle.backend.lego.register;

import com.github.firmwehr.gentle.backend.lego.LegoPlate;
import com.github.firmwehr.gentle.backend.lego.LegoParentBløck;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoConst;
import com.github.firmwehr.gentle.backend.lego.nodes.LegoNode;
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

	public boolean doInterfere(LegoNode first, LegoNode second) {
		LegoNode t;
		LegoNode b;
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

		for (LegoNode use : uses.uses(t)) {
			if (dominance.dominates(b, use)) {
				return true;
			}
		}

		return false;
	}

	public /* N */ Set<LegoNode> interferenceNeighbours(LegoNode x) {
		Set<LegoNode> N = new HashSet<>();
		Set<LegoPlate> visited = new HashSet<>();

		for (LegoNode use : uses.uses(x)) {
			findInt(use.block(), x, N, visited);
		}

		return N.stream()
			.filter(it -> !(it instanceof LegoConst) && !it.registerIgnore())
			.filter(it -> !it.equals(x))
			.collect(Collectors.toSet());
	}

	private void findInt(LegoPlate block, LegoNode x, Set<LegoNode> N, Set<LegoPlate> visited) {
		visited.add(block);
		Set<LegoNode> liveouts = new HashSet<>(liveliness.getLiveOut(block));

		for (LegoNode node : Lists.reverse(block.nodes())) {
			if (node.equals(x)) {
				break;
			}
			liveouts.remove(node);
			liveouts.addAll(node.inputs());

			if (liveouts.contains(x)) {
				N.addAll(liveouts);
			}
		}

		for (LegoParentBløck parent : block.parents()) {
			LegoPlate parentBlock = parent.parent();
			LegoNode lastInParent = parentBlock.nodes().get(parentBlock.nodes().size() - 1);
			if (dominance.dominates(x, lastInParent) && !visited.contains(parentBlock)) {
				findInt(parentBlock, x, N, visited);
			}
		}
	}
}
