package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;

public sealed interface Token
		permits TokenComment, TokenEndOfFile, TokenIdentifier, TokenIntegerLiteral, TokenKeyword, TokenWhitespace {
	
	TokenType tokenType();
	
	SourcePosition position();
}
