package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.Mode;
import firm.nodes.Node;

import java.util.HashMap;
import java.util.function.Predicate;

public class ModeFilter implements NodeFilter {
	private final String key;
	private final Predicate<Mode> modeFilter;
	private final NodeFilter underlying;

	public ModeFilter(String key, Predicate<Mode> modeFilter, NodeFilter underlying) {
		this.key = key;
		this.modeFilter = modeFilter;
		this.underlying = underlying;
	}

	@Override
	public boolean matches(Node node) {
		if (!underlying.matches(node)) {
			return false;
		}
		return modeFilter.test(node.getMode());
	}

	@Override
	public void storeMatch(HashMap<String, Node> matches, Node matchedNode) {
		underlying.storeMatch(matches, matchedNode);
		matches.put(key, matchedNode);
	}
}
