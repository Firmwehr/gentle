package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;

public record TokenEndOfFile(SourcePosition position) implements Token {
	
	public static TokenEndOfFile create(LexReader reader) throws LexerException {
		var position = reader.position();
		if (!reader.isEndOfInput()) {
			throw new LexerException("no at end of input", reader);
		}
		return new TokenEndOfFile(position);
	}
	
	@Override
	public TokenType tokenType() {
		return TokenType.EOF;
	}
}
