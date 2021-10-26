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
	LEFT_PAREN("("),
	RIGHT_PAREN(")"),
	ASSIGN_MULTIPLY("*="),
	MULTIPLY("*"),
	INCREMENT("++"),
	ADD_AND_ASSIGN("+="),
	PLUS("+"),
	COMMA(","),
	ASSIGN_SUBTRACT("-="),
	DECREMENT("--"),
	MINUS("-"),
	DOT("."),
	ASSIGN_DIVIDE("/="),
	DIVIDE("/"),
	COLON(":"),
	SEMICOLON(";"),
	ASSIGN_LEFT_SHIFT("<<="),
	LEFT_SHIFT("<<"),
	LESS_OR_EQUAL("<="),
	LESS_THAN("<"),
	EQUAL("=="),
	ASSIGN("="),
	GREATER_OR_EQUAL(">="),
	ASSIGN_SIGNED_RIGHT_SHIFT(">>="),
	ASSIGN_UNSIGNED_RIGHT_SHIFT(">>>="),
	UNSIGNED_RIGHT_SHIFT(">>>"),
	SIGNED_RIGHT_SHIFT(">>"),
	GREATER_THAN(">"),
	QUESTION_MARK("?"),
	ASSIGN_MODULO("%="),
	MODULO("%"),
	ASSIGN_BITWISE_AND("&="),
	LOGICAL_AND("&&"),
	BITWISE_AND("&"),
	LEFT_BRACKET("["),
	RIGHT_BRACKET("]"),
	ASSIGN_BITWISE_XOR("^="),
	BITWISE_XOR("^"),
	LEFT_BRACE("{"),
	RIGHT_BRACE("}"),
	BITWISE_NOT("~"),
	ASSIGN_BITWISE_OR("|="),
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
