package com.github.firmwehr.gentle.parser;

// Invariants:
// type == INTEGER_LITERAL ==> parseInt(text) doesn't throw
public record Token(TokenType type, String text) {
	
	@Override
	public String toString() {
		switch (this.type()) {
			case EOF:
			case COMMENT:
			case WHITESPACE:
				return this.type().toString();
			case IDENTIFIER:
			case INTEGER_LITERAL:
				return String.format("%s[%s]", this.type, this.text);
			default:
				return this.text;
		}
	}
}