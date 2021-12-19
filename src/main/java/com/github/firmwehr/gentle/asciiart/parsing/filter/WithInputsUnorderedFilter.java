package com.github.firmwehr.gentle.asciiart.parsing.filter;

import com.google.common.collect.Iterables;
import firm.nodes.Node;

import java.util.Collection;

public class WithInputsUnorderedFilter<T extends Node> implements NodeFilter<T> {
	private final NodeFilter<T> underlying;
	private final Collection<NodeFilter<T>> inputs;

	public WithInputsUnorderedFilter(NodeFilter<T> underlying, Collection<NodeFilter<T>> inputs) {
		this.underlying = underlying;
		this.inputs = inputs;
	}

	@Override
	public Class<T> type() {
		return underlying.type();
	}

	@Override
	public boolean test(Node node) {
		if (!underlying.test(node)) {
			return false;
		}
		Node[] preds = Iterables.toArray(node.getPreds(), Node.class);
		if (preds.length != inputs.size()) {
			return false;
		}

		for (Node pred : preds) {
			boolean foundFilter = false;
			for (NodeFilter<T> filter : inputs) {
				if (filter.test(pred)) {
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
	public T convert(Node node) {
		return underlying.convert(node);
	}
}
