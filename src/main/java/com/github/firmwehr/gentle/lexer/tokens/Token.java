package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.lexer.tokens.descriptors.TokenDescriptor;

public abstract class Token {
	
	private final TokenDescriptor descriptor;
	private final SourcePosition position;
	
	protected Token(TokenDescriptor descriptor, SourcePosition position) {
		this.descriptor = descriptor;
		this.position = position;
	}
	
	public final TokenType tokenType() {
		return descriptor.tokenType();
	}
	
	public final SourcePosition position() {
		return position;
	}
}
