package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record TokenIdentifier(
	SourceSpan sourceSpan,
	String id
) implements Token {

	public static TokenIdentifier create(LexReader reader) throws LexerException {
		if (!Character.isJavaIdentifierStart(reader.peek())) {
			throw new LexerException("does not start with identifier codepoint", reader);
		}
		var startPos = reader.position();
		var id = reader.readUntilOrEndOfFile(cp -> !Character.isJavaIdentifierPart(cp), false);
		return new TokenIdentifier(new SourceSpan(startPos, reader.endPositionOfRead()), id);
	}

	@Override
	public String format() {
		return "identifier " + id;
	}

	@Override
	public TokenType tokenType() {
		return TokenType.IDENTIFIER;
	}
}
