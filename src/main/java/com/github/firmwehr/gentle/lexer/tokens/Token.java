package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;
import com.github.firmwehr.gentle.source.SourceSpan;

public sealed interface Token
	permits TokenComment, TokenEndOfFile, TokenIdentifier, TokenIntegerLiteral, TokenKeyword, TokenWhitespace {

	/**
	 * Used by lextest command to print parsed lex tokens. The format of the returned string is equivalent to the
	 * required format given by task sheet 2. For custom tokens, the returned string tries to resemble the actual token
	 * as close as possible (if possible :)
	 *
	 * @return A string representation of this token.
	 */
	String format();

	TokenType tokenType();

	SourceSpan sourceSpan();
}
