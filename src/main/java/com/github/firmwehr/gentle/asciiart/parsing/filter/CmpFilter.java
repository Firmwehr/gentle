package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.Relation;
import firm.nodes.Cmp;
import firm.nodes.Node;

import java.util.HashMap;

public class CmpFilter implements NodeFilter {
	private final String key;
	private final firm.Relation relation;

	public CmpFilter(String key, Relation relation) {
		this.key = key;
		this.relation = relation;
	}

	@Override
	public boolean matches(Node node) {
		if (node.getClass() != Cmp.class) {
			return false;
		}

		return relation == ((Cmp) node).getRelation();
	}

	@Override
	public void storeMatch(HashMap<String, Node> matches, Node matchedNode) {
		matches.put(key, matchedNode);
	}
}
