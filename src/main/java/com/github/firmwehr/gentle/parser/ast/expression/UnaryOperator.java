package com.github.firmwehr.gentle.parser.ast.expression;

public enum UnaryOperator {
	LOGICAL_NOT("!"),
	NEGATION("-"),
	;

	private final String name;

	UnaryOperator(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
