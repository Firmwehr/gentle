package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.lexer.tokens.descriptors.KeywordDescriptor;
import com.github.firmwehr.gentle.lexer.tokens.descriptors.TokenDescriptor;

public class TokenVoid extends Token {
	
	public static final TokenDescriptor DESCRIPTOR = new KeywordDescriptor(TokenType.VOID, "void", TokenVoid::new);
	
	private TokenVoid(SourcePosition position) {
		super(DESCRIPTOR, position);
	}
}
