package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.Mode;
import firm.nodes.Node;

import java.util.function.Predicate;

public class ModeFilter<T extends Node> implements NodeFilter<T> {
	private final Predicate<Mode> modeFilter;
	private final NodeFilter<T> underlying;

	public ModeFilter(Predicate<Mode> modeFilter, NodeFilter<T> underlying) {
		this.modeFilter = modeFilter;
		this.underlying = underlying;
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
		return modeFilter.test(node.getMode());
	}

	@Override
	public T convert(Node node) {
		return underlying.convert(node);
	}
}
