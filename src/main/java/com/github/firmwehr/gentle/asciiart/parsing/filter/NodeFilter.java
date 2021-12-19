package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;

import java.util.HashMap;

public interface NodeFilter {

	boolean matches(Node node);

	void storeMatch(HashMap<String, Node> matches, Node matchedNode);
}
