package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public final class TokenEndOfFile extends Token {
	
	private TokenEndOfFile(SourcePosition position) {
		super(TokenType.EOF, position);
	}
	
	@Override
	public String toString() {
		return "TokenEndOfFile";
	}
	
	public static TokenEndOfFile create(LexReader reader) throws LexerException {
		var position = reader.position();
		if (!reader.isEndOfInput()) {
			throw new LexerException("no at end of input", reader);
		}
		return new TokenEndOfFile(position);
	}
}
