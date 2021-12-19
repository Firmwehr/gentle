package com.github.firmwehr.gentle.asciiart.parsing.filter;

import com.google.common.collect.Iterables;
import firm.nodes.Node;

import java.util.HashMap;
import java.util.List;

public class WithInputsOrderedFilter implements NodeFilter {
	private final String key;
	private final NodeFilter underlying;
	private final List<NodeFilter> inputs;

	public WithInputsOrderedFilter(String key, NodeFilter underlying, List<NodeFilter> inputs) {
		this.key = key;
		this.underlying = underlying;
		this.inputs = inputs;
	}

	@Override
	public boolean matches(Node node) {
		if (!underlying.matches(node)) {
			return false;
		}
		Node[] preds = Iterables.toArray(node.getPreds(), Node.class);
		if (preds.length != inputs.size()) {
			return false;
		}

		for (int i = 0; i < preds.length; i++) {
			if (!inputs.get(i).matches(preds[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void storeMatch(HashMap<String, Node> matches, Node matchedNode) {
		Node[] preds = Iterables.toArray(matchedNode.getPreds(), Node.class);
		for (int i = 0; i < preds.length; i++) {
			inputs.get(i).storeMatch(matches, preds[i]);
		}

		matches.put(key, matchedNode);
	}
}
