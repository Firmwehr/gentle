package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.Token;
import com.github.firmwehr.gentle.lexer.tokens.TokenVoid;
import com.github.firmwehr.gentle.lexer.tokens.descriptors.TokenDescriptor;

import java.util.Arrays;
import java.util.Optional;

public enum TokenType {
	VOID(TokenVoid.DESCRIPTOR);
	
	private final TokenDescriptor descriptor;
	
	TokenType(TokenDescriptor descriptor) {
		if (this != descriptor.tokenType())
			throw new Error("malformed token definition for %s, descriptor does not match assigned token type, please check enum definition".formatted(name()));
		this.descriptor = descriptor;
	}
	
	public static Token parseNextToken(LexReader reader) throws LexReader.LexerException {
		
		// try all known tokens until first one matches current input
		return Arrays.stream(values())
				.map((tokenType -> attemptParse(reader, tokenType)))
				.flatMap(Optional::stream)
				.findFirst()
				.orElseThrow(() -> new LexReader.LexerException("unable to find suitable token", reader));
	}
	
	private static Optional<Token> attemptParse(LexReader reader, TokenType tokenType) {
		try {
			var token = tokenType.descriptor.attemptParse(reader);
			if (token.tokenType() != tokenType) {
				throw new Error("Descriptor for %s created token of wrong type, please check enum definition".formatted(tokenType));
			}
			return Optional.of(token);
		} catch (LexReader.LexerException ignore) {
			return Optional.empty();
		}
	}
	
}
