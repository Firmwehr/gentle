package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public final class TokenIdentifier extends Token {
	
	public final String id;
	
	private TokenIdentifier(SourcePosition position, String id) {
		super(TokenType.IDENTIFIER, position);
		this.id = id;
	}
	
	@Override
	public String toString() {
		return "TokenIdentifier{" +
				"id='" + id + '\'' +
				'}';
	}
	
	public static TokenIdentifier create(LexReader reader) throws LexerException {
		if (!Character.isJavaIdentifierStart(reader.peek()))
			throw new LexerException("does not start with identifier codepoint", reader);
		var id = reader.readUntil(cp -> !Character.isJavaIdentifierPart(cp), false);
		return new TokenIdentifier(reader.position(), id);
	}
}
