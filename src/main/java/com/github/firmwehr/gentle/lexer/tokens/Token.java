package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;

public sealed interface Token
	permits TokenComment, TokenEndOfFile, TokenIdentifier, TokenIntegerLiteral, TokenKeyword, TokenWhitespace {

	/**
	 * Used by lextest command to print parsed lex tokens. The returned string is equivalent to the parse string.
	 *
	 * @return A string representation of this token.
	 */
	String format();

	TokenType tokenType();

	SourcePosition position();
}
