package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.Token;
import com.github.firmwehr.gentle.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lexer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Lexer.class);
	
	private LexReader reader;
	
	public Lexer(Source source) {
		reader = new LexReader(source);
	}
	
	public Token nextToken() throws LexerException {
		var parse = TokenType.parseNextToken(reader);
		var childReader = parse.reader();
		var token = parse.token();
		var diff = reader.diff(childReader);
		
		if (diff.isEmpty() && token.tokenType() != TokenType.EOF) { // eof is allowed to read empty token
			throw new Error("parsed token from empty string, this is an error in the code");
		}
		
		LOGGER.trace("emitting token {} from string slice @ {}: '{}'", token, reader.position().format(), diff);
		reader = childReader;
		return token;
	}
}
