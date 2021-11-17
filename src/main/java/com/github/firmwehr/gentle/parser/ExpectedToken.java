package com.github.firmwehr.gentle.parser;

public enum ExpectedToken {
	// Keywords
	CLASS("class", true),
	ELSE("else", true),
	IF("if", true),
	NEW("new", true),
	NULL("null", true),
	PUBLIC("public", true),
	RETURN("return", true),
	STATIC("static", true),
	THIS("this", true),
	THROWS("throws", true),
	VOID("void", true),
	WHILE("while", true),

	// Operators
	ASSIGN("=", true),
	COMMA(",", true),
	DOT(".", true),
	LEFT_BRACE("{", true),
	LEFT_BRACKET("[", true),
	LEFT_PAREN("(", true),
	LOGICAL_NOT("!", true),
	MINUS("-", true),
	RIGHT_BRACE("}", true),
	RIGHT_BRACKET("]", true),
	RIGHT_PAREN(")", true),
	SEMICOLON(";", true),

	// Categories,
	BASIC_TYPE("basic type", false),
	BINARY_OPERATOR("binary operator", false),
	BOOLEAN_LITERAL("boolean literal", false),
	IDENTIFIER("identifier", false),
	INTEGER_LITERAL("integer literal", false),

	// Misc,
	EOF("EOF", false);

	private final String name;
	private final boolean literally; // or operator

	ExpectedToken(String name, boolean literally) {
		this.name = name;
		this.literally = literally;
	}

	public String getDescription() {
		if (literally) {
			return "'" + name + "'";
		} else {
			return name;
		}
	}
}
