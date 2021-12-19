package com.github.firmwehr.gentle.asciiart.parsing.filter;

import com.google.common.collect.Iterables;
import firm.nodes.Node;

import java.util.List;

public class WithInputsOrderedFilter<T extends Node> implements NodeFilter<T> {
	private final NodeFilter<T> underlying;
	private final List<NodeFilter<T>> inputs;

	public WithInputsOrderedFilter(NodeFilter<T> underlying, List<NodeFilter<T>> inputs) {
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

		for (int i = 0; i < preds.length; i++) {
			if (!inputs.get(i).test(preds[i])) {
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
