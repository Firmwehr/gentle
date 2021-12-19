package com.github.firmwehr.gentle.asciiart.parsing.filter;

import firm.nodes.Const;
import firm.nodes.Node;

import java.util.OptionalLong;

public class ConstFilter implements NodeFilter<Const> {
	private final OptionalLong value;

	public ConstFilter(OptionalLong value) {
		this.value = value;
	}

	@Override
	public Class<Const> type() {
		return Const.class;
	}

	@Override
	public boolean test(Node node) {
		if (node.getClass() != Const.class) {
			return false;
		}
		if (value.isEmpty()) {
			return true;
		}

		return value.getAsLong() == ((Const) node).getTarval().asLong();
	}
}
