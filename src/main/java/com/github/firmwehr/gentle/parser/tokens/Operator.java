package com.github.firmwehr.gentle.parser.tokens;

/**
 * All operators and separators from the MiniJava language report.
 * <p>
 * Naming mostly follows the token's function, unless it is ambiguous orpart of a larger piece of functionality. In
 * those cases, the token is named after its appearance.
 */
public enum Operator {
	NOT_EQUAL("!="),
	LOGICAL_NOT("!"),
	OPENING_PARENTHESIS("("),
	CLOSING_PARENTHESIS(")"),
	MULTIPLY_AND_ASSIGN("*="),
	MULTIPLY("*"),
	INCREASE_BY_ONE("++"),
	AND_AND_ASSIGN("+="),
	PLUS("+"),
	COMMA(","),
	SUBTRACT_AND_ASSIGN("-="),
	DECREASE_BY_ONE("--"),
	MINUS("-"),
	PERIOD("."),
	DIVIDE_AND_ASSIGN("/="),
	DIVIDE("/"),
	COLON(":"),
	SEMICOLON(";"),
	LEFT_SHIFT_AND_ASSIGN("<<="),
	LEFT_SHIFT("<<"),
	LESS_THAN_OR_EQUAL("<="),
	LESS_THAN("<"),
	EQUAL("=="),
	ASSIGN("="),
	GREATER_THAN_OR_EQUAL(">="),
	SIGNED_RIGHT_SHIFT_AND_ASSIGN(">>="),
	UNSIGNED_RIGHT_SHIFT_AND_ASSIGN(">>>="),
	UNSIGNED_RIGHT_SHIFT(">>>"),
	SIGNED_RIGHT_SHIFT(">>"),
	GREATER_THAN(">"),
	QUESTION_MARK("?"),
	MODULO_AND_ASSIGN("%="),
	MODULO("%"),
	BITWISE_AND_AND_ASSIGN("&="),
	LOGICAL_AND("&&"),
	BITWISE_AND("&"),
	OPENING_BRACKET("["),
	CLOSING_BRACKET("]"),
	BITWISE_XOR_AND_ASSIGN("^="),
	BITWISE_XOR("^"),
	OPENING_BRACE("{"),
	CLOSING_BRACE("}"),
	BITWISE_NOT("~"),
	BITWISE_OR_AND_ASSIGN("|="),
	LOGICAL_OR("||"),
	BITWISE_OR("|"),
	;

	private final String name;

	Operator(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
