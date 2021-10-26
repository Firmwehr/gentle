package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;

public record TokenWhitespace(
	SourcePosition position,
	String whitespaces
) implements Token {

	public static TokenWhitespace create(LexReader reader) throws LexerException {
		var position = reader.position();
		if (!Character.isWhitespace(reader.peek())) {
			throw new LexerException("not a whitespace", reader);
		}
		return new TokenWhitespace(position,
			reader.readUntilOrEndOfFile(cp -> !Character.isWhitespace(cp) || reader.isEndOfInput(), false));
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
