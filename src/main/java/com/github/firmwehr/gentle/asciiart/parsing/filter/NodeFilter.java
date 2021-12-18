package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;

public interface NodeFilter<T extends Node> {

	Class<T> type();

	boolean test(Node node);

	T convert(Node node);
}
