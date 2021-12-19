package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;

import java.util.HashMap;

public class ClassFilter implements NodeFilter {
	private final Class<?> clazz;
	private final String key;

	public ClassFilter(String key, Class<?> clazz) {
		this.clazz = clazz;
		this.key = key;
	}

	@Override
	public boolean matches(Node node) {
		return clazz == node.getClass();
	}

	@Override
	public void storeMatch(HashMap<String, Node> matches, Node matchedNode) {
		matches.put(key, matchedNode);
	}
}
