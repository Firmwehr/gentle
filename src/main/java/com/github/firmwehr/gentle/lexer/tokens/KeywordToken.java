package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public class KeywordToken extends GentleToken {
	
	private KeywordToken(TokenType tokenType, SourcePosition position) {
		super(tokenType, position);
	}
	
	@Override
	public String toString() {
		return "KeywordToken{type=%s}".formatted(tokenType());
	}
	
	public static KeywordToken create(LexReader reader, TokenType tokenType, String keyword) throws LexerException {
		var position = reader.position();
		
		// this is cursed, but it works
		// checks if keyword contains characters that are part of an identifier
		var containsIdentifiers = keyword.codePoints().anyMatch(Character::isJavaIdentifierPart);
		
		// match on keyword
		reader.expect(keyword);
		
		// if keyword contains identifier characters, make sure that what follows afterwards is NOT an identifier
		if (containsIdentifiers) {
			if (!reader.isEndOfInput() && Character.isJavaIdentifierPart(reader.peek())) {
				throw new LexerException("followed by another identifier character", reader);
			}
		}
		
		return new KeywordToken(tokenType, position);
	}
}
