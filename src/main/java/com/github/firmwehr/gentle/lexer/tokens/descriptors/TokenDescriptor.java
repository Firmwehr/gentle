package com.github.firmwehr.gentle.lexer.tokens.descriptors;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.lexer.tokens.Token;

public interface TokenDescriptor {
	
	TokenType tokenType();
	
	Token attemptParse(LexReader reader) throws LexReader.LexerException;
}
