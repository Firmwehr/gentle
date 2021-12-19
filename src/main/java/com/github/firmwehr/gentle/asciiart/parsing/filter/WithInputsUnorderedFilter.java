package com.github.firmwehr.gentle.asciiart.parsing.filter;

import com.google.common.collect.Iterables;
import firm.nodes.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class WithInputsUnorderedFilter implements NodeFilter {
	private final String key;
	private final NodeFilter underlying;
	private final Collection<NodeFilter> inputs;

	public WithInputsUnorderedFilter(String key, NodeFilter underlying, Collection<NodeFilter> inputs) {
		this.key = key;
		this.underlying = underlying;
		this.inputs = inputs;
	}

	@Override
	public boolean matches(Node node) {
		if (!underlying.matches(node)) {
			return false;
		}
		return matchesAndDo(node, (filter, pred) -> {
		});
	}

	private boolean matchesAndDo(Node node, BiConsumer<NodeFilter, Node> action) {
		Node[] preds = Iterables.toArray(node.getPreds(), Node.class);
		if (preds.length != inputs.size()) {
			return false;
		}

		for (Node pred : preds) {
			boolean foundFilter = false;
			for (NodeFilter filter : inputs) {
				if (filter.matches(pred)) {
					action.accept(filter, pred);
					foundFilter = true;
					break;
				}
			}

			if (!foundFilter) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void storeMatch(HashMap<String, Node> matches, Node matchedNode) {
		matchesAndDo(matchedNode, (nodeFilter, node) -> nodeFilter.storeMatch(matches, node));
		matches.put(key, matchedNode);
	}
}
