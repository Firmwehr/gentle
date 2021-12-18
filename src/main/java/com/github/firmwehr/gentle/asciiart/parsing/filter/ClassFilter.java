package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;

public class ClassFilter<T extends Node> implements NodeFilter<T> {
	private final Class<T> clazz;

	public ClassFilter(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Class<T> type() {
		return clazz;
	}

	@Override
	public boolean test(Node node) {
		return clazz == node.getClass();
	}

	@Override
	public T convert(Node node) {
		return clazz.cast(node);
	}
}
