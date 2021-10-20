package com.github.firmwehr.gentle.lexer.tokens.descriptors;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.lexer.tokens.Token;
import com.google.common.base.Preconditions;

import java.util.function.Function;

/**
 * Descriptor for common case of describin a single keyword token with ease.
 */
public record KeywordDescriptor(TokenType tokenType, String keyword,
								Function<SourcePosition, Token> constructor) implements TokenDescriptor {
	
	public KeywordDescriptor {
		Preconditions.checkArgument(!keyword.isEmpty(), "must not be empty");
		Preconditions.checkArgument(keyword.strip().equals(keyword), "must not contain whitespaces");
	}
	
	@Override
	public Token attemptParse(LexReader reader) throws LexReader.LexerException {
		reader.expect(keyword);
		return constructor.apply(reader.position());
	}
}
