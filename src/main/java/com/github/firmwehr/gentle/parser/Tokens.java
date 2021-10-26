package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.tokens.EofToken;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;
import com.github.firmwehr.gentle.parser.tokens.Keyword;
import com.github.firmwehr.gentle.parser.tokens.Operator;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Tokens {
	private final Source source;
	private final List<Token> tokens;
	private final EofToken lastToken;
	private int index;

	public Tokens(Source source, List<Token> tokens, EofToken lastToken) {
		this.source = source;
		this.tokens = tokens;
		this.lastToken = lastToken;
		this.index = 0;
	}

	public static Tokens fromLexer(Source source, Lexer lexer) throws LexerException {
		List<Token> tokens = new ArrayList<>();
		while (true) {
			Token token = Token.fromLexerToken(lexer.nextToken());
			if (token instanceof EofToken eof) {
				return new Tokens(source, tokens, eof);
			} else {
				tokens.add(token);
			}
		}
	}

	public <T> T error(String description) throws ParseException {
		throw new ParseException(source, peek(), description);
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

	public void expectKeyword(Keyword keyword) throws ParseException {
		if (peek().isKeyword(keyword)) {
			take();
		} else {
			error("Expected keyword " + keyword.getName());
		}
	}

	public void expectOperator(Operator operator) throws ParseException {
		if (peek().isOperator(operator)) {
			take();
		} else {
			error("Expected operator " + operator.getName());
		}
	}

	public IdentToken expectIdent() throws ParseException {
		Optional<IdentToken> identToken = peek().asIdentToken();
		if (identToken.isPresent()) {
			take();
			return identToken.get();
		} else {
			return error("Expected identifier");
		}
	}
}
