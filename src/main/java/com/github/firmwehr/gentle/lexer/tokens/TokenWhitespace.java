package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

import java.text.MessageFormat;

public final class TokenWhitespace extends GentleToken {
	
	public final String whitespaces;
	
	private TokenWhitespace(SourcePosition position, String whitespaces) {
		super(TokenType.WHITESPACE, position);
		this.whitespaces = whitespaces;
	}
	
	@Override
	public String toString() {
		return MessageFormat.format("TokenWhitespace'{'length={0}'}'", whitespaces.length());
	}
	
	public static TokenWhitespace create(LexReader reader) throws LexerException {
		var position = reader.position();
		if (!Character.isWhitespace(reader.peek()))
			throw new LexerException("not a whitespace", reader);
		return new TokenWhitespace(position, reader.readUntilOrEndOfFile(
				cp -> !Character.isWhitespace(cp) || reader.isEndOfInput(), false));
	}
}
