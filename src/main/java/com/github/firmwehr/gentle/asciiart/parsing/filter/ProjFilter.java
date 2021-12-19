package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Node;
import firm.nodes.Proj;

import java.util.OptionalInt;

public class ProjFilter implements NodeFilter<Proj> {
	private final OptionalInt number;

	public ProjFilter(OptionalInt number) {
		this.number = number;
	}

	@Override
	public Class<Proj> type() {
		return Proj.class;
	}

	@Override
	public boolean test(Node node) {
		if (node.getClass() != Proj.class) {
			return false;
		}
		if (number.isEmpty()) {
			return true;
		}
		return ((Proj) node).getNum() == number.getAsInt();
	}

}
