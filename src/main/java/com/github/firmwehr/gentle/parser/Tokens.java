package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.tokens.EofToken;
import com.github.firmwehr.gentle.parser.tokens.Token;

import java.util.ArrayList;
import java.util.List;

public class Tokens {
	private final List<Token> tokens;
	private final EofToken lastToken;
	private int index;

	public Tokens(List<Token> tokens, EofToken lastToken) {
		this.tokens = tokens;
		this.lastToken = lastToken;
		this.index = 0;
	}

	public static Tokens fromLexer(Lexer lexer) throws LexerException {
		List<Token> tokens = new ArrayList<>();
		while (true) {
			Token token = Token.fromLexerToken(lexer.nextToken());
			if (token instanceof EofToken eof) {
				return new Tokens(tokens, eof);
			} else {
				tokens.add(token);
			}
		}
	}

	public void take(int n) {
		if (n < 1) {
			throw new IllegalArgumentException("n must be greater than 0");
		}

		index = Math.min(tokens.size(), index + n);
	}

	public void take() {
		take(1);
	}

	public Token peek(int offset) {
		if (offset < 0) {
			throw new IllegalArgumentException("offset must not be negative");
		}

		var i = index + offset;
		if (i < tokens.size()) {
			return tokens.get(i);
		} else {
			return lastToken;
		}
	}

	public Token peek() {
		return peek(0);
	}
}
