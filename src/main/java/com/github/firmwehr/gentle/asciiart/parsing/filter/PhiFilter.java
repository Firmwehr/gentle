package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;
import firm.nodes.Phi;

import java.util.Optional;

public class PhiFilter implements NodeFilter<Phi> {
	private final Optional<Boolean> shouldbeLoop;

	public PhiFilter(Optional<Boolean> shouldbeLoop) {
		this.shouldbeLoop = shouldbeLoop;
	}

	@Override
	public Class<Phi> type() {
		return Phi.class;
	}

	@Override
	public boolean test(Node node) {
		if (node.getClass() != Phi.class) {
			return false;
		}
		if (shouldbeLoop.isEmpty()) {
			return true;
		}
		return shouldbeLoop.get().equals(((Phi) node).getLoop() == 1);
	}
}
