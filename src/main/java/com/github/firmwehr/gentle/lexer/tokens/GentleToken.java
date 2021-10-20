package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.TokenType;

public abstract class GentleToken {
	
	private final TokenType tokenType;
	private final SourcePosition position;
	
	protected GentleToken(TokenType tokenType, SourcePosition position) {
		this.tokenType = tokenType;
		this.position = position;
	}
	public final TokenType tokenType() {
		return tokenType;
	}
	
	public final SourcePosition position() {
		return position;
	}
}
