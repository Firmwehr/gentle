package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.GentleToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GentleLexer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GentleLexer.class);
	
	private LexReader reader;
	
	public GentleLexer(String input) {
		reader = new LexReader(input);
	}
	
	public GentleToken nextToken() throws LexerException {
		var parse = TokenType.parseNextToken(reader);
		var childReader = parse.reader();
		var token = parse.token();
		var diff = reader.diff(childReader);
		
		if (diff.isEmpty() && token.tokenType() != TokenType.EOF) { // eof is allowed to read empty token
			throw new Error("parsed token from empty string, this is an error in the code");
		}
		
		LOGGER.trace("emiting token {} from string slice @ {}: '{}'", token, reader.position().print(), diff);
		reader = childReader;
		return token;
	}
}
