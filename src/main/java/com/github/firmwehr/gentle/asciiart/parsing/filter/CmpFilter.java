package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.Relation;
import firm.nodes.Cmp;
import firm.nodes.Node;

import java.util.Optional;

public class CmpFilter implements NodeFilter<Cmp> {
	private final Optional<firm.Relation> relation;

	public CmpFilter(Optional<Relation> relation) {
		this.relation = relation;
	}

	@Override
	public Class<Cmp> type() {
		return Cmp.class;
	}

	@Override
	public boolean test(Node node) {
		if (node.getClass() != Cmp.class) {
			return false;
		}
		if (relation.isEmpty()) {
			return true;
		}

		return relation.get() == ((Cmp) node).getRelation();
	}

	@Override
	public Cmp convert(Node node) {
		return (Cmp) node;
	}
}
