package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public record TokenIdentifier(SourcePosition position, String id) implements Token {
	
	public static TokenIdentifier create(LexReader reader) throws LexerException {
		if (!Character.isJavaIdentifierStart(reader.peek()))
			throw new LexerException("does not start with identifier codepoint", reader);
		var id = reader.readUntil(cp -> !Character.isJavaIdentifierPart(cp), false);
		return new TokenIdentifier(reader.position(), id);
	}
	
	@Override
	public TokenType tokenType() {
		return TokenType.IDENTIFIER;
	}
}
