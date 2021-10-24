package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public record TokenKeyword(TokenType tokenType, SourcePosition position) implements Token {
	
	public static TokenKeyword create(LexReader reader, TokenType tokenType, String keyword) throws LexerException {
		var position = reader.position();
		
		// this is cursed, but it works
		// checks if keyword contains characters that are part of an identifier (this is the if the first codepoint is a valid identifier)
		// this is important because the result of this check decides on which symbols we terminate the current token and start a new one
		var containsIdentifiers = keyword.codePoints().anyMatch(Character::isJavaIdentifierStart);
		
		// match on keyword
		reader.expect(keyword);
		
		// if keyword contains identifier characters, make sure that what follows afterwards is NOT an identifier
		// this prevents cases in which we parse "void0" as "void" followed by "0"
		if (containsIdentifiers) {
			if (!reader.isEndOfInput() && Character.isJavaIdentifierPart(reader.peek())) {
				throw new LexerException("followed by another identifier character", reader);
			}
		}
		
		return new TokenKeyword(tokenType, position);
	}
}
