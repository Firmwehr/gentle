package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record TokenWhitespace(
	SourceSpan sourceSpan,
	String whitespaces
) implements Token {

	public static TokenWhitespace create(LexReader reader) throws LexerException {
		var startPos = reader.position();
		if (!Character.isWhitespace(reader.peek())) {
			throw new LexerException("not a whitespace", reader);
		}
		var w = reader.readUntilOrEndOfFile(cp -> !Character.isWhitespace(cp) || reader.isEndOfInput(), false);
		return new TokenWhitespace(reader.span(startPos), w);
	}

	@Override
	public String format() {
		return "whitespace";
	}

	@Override
	public TokenType tokenType() {
		return TokenType.WHITESPACE;
	}
}
