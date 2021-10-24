package com.github.firmwehr.gentle.parser.ast.expression;

public enum BinaryOperator {
	ASSIGNMENT("=", 0),

	LOGICAL_OR("||", 1),

	LOGICAL_AND("&&", 2),

	EQUALITY("==", 3),
	INEQUALITY("!=", 3),

	LESS_THAN("<", 4),
	LESS_THAN_OR_EQUAL("<=", 4),
	GREATER_THAN(">", 4),
	GREATER_THAN_OR_EQUAL(">=", 4),

	ADDITION("+", 5),
	SUBTRACTION("-", 5),

	MULTIPLICATION("*", 6),
	DIVISION("/", 6),
	MODULO("%", 6),
	;

	private final String name;
	private final int precedence;

	BinaryOperator(String name, int precedence) {
		this.name = name;
		this.precedence = precedence;
	}

	public String getName() {
		return name;
	}

	public int getPrecedence() {
		return precedence;
	}
}
