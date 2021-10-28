package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.tokens.Operator;

import java.util.Arrays;
import java.util.Optional;

public enum BinaryOperator {
	ASSIGN("=", 0, Associativity.RIGHT, Operator.ASSIGN),

	LOGICAL_OR("||", 1, Associativity.LEFT, Operator.LOGICAL_OR),

	LOGICAL_AND("&&", 2, Associativity.LEFT, Operator.LOGICAL_AND),

	EQUAL("==", 3, Associativity.LEFT, Operator.EQUAL),
	NOT_EQUAL("!=", 3, Associativity.LEFT, Operator.NOT_EQUAL),

	LESS_THAN("<", 4, Associativity.LEFT, Operator.LESS_THAN),
	LESS_OR_EQUAL("<=", 4, Associativity.LEFT, Operator.LESS_OR_EQUAL),
	GREATER_THAN(">", 4, Associativity.LEFT, Operator.GREATER_THAN),
	GREATER_OR_EQUAL(">=", 4, Associativity.LEFT, Operator.GREATER_OR_EQUAL),

	ADD("+", 5, Associativity.LEFT, Operator.PLUS),
	SUBTRACT("-", 5, Associativity.LEFT, Operator.MINUS),

	MULTIPLY("*", 6, Associativity.LEFT, Operator.MULTIPLY),
	DIVIDE("/", 6, Associativity.LEFT, Operator.DIVIDE),
	MODULO("%", 6, Associativity.LEFT, Operator.MODULO),
	;

	private final String name;
	private final int precedence;
	private final Associativity associativity;
	// The corresponding operator token type
	private final Operator operator;

	BinaryOperator(String name, int precedence, Associativity associativity, Operator operator) {
		this.name = name;
		this.precedence = precedence;
		this.associativity = associativity;
		this.operator = operator;
	}

	public static Optional<BinaryOperator> fromOperator(Operator operator) {
		return Arrays.stream(values()).filter(binop -> binop.getOperator() == operator).findFirst();
	}

	public String getName() {
		return name;
	}

	public int getPrecedence() {
		return precedence;
	}

	public Associativity getAssociativity() {
		return associativity;
	}

	public Operator getOperator() {
		return operator;
	}

	public enum Associativity {
		LEFT,
		RIGHT
	}

}
