package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;
import com.github.firmwehr.gentle.source.SourceSpan;

public record TokenEndOfFile(SourceSpan sourceSpan) implements Token {

	public static TokenEndOfFile create(LexReader reader) throws LexerException {
		var position = reader.position();
		if (!reader.isEndOfInput()) {
			throw new LexerException("no at end of input", reader);
		}
		return new TokenEndOfFile(new SourceSpan(position, position));
	}

	@Override
	public String format() {
		return "EOF";
	}

	@Override
	public TokenType tokenType() {
		return TokenType.EOF;
	}
}
