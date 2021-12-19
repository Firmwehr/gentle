package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;

import java.util.Map;

public interface NodeFilter<T extends Node> {

	Class<T> type();

	boolean test(Node node);

	default void storeMatch(Map<NodeFilter<T>, Node> map, Node node) {
		map.put(this, node);
	}
}
