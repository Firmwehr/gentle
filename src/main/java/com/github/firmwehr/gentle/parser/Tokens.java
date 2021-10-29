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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Tokens {
	private final Source source;
	private final List<Token> tokens;
	private final EofToken lastToken;

	private int index;
	private Set<String> expectedTokensAtIndex;

	public Tokens(Source source, List<Token> tokens, EofToken lastToken) {
		this.source = source;
		this.tokens = tokens;
		this.lastToken = lastToken;

		index = 0;
		expectedTokensAtIndex = new HashSet<>();
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

	public <T> T error() throws ParseException {
		List<String> expectedTokens = expectedTokensAtIndex.stream().sorted().collect(Collectors.toList());

		String description;
		if (expectedTokens.size() == 0) {
			description = "Something went wrong";
		} else if (expectedTokens.size() == 1) {
			description = "Expected " + expectedTokens.get(0);
		} else {
			description = "Expected " +
				expectedTokens.stream().limit(expectedTokens.size() - 1).collect(Collectors.joining(", ")) + " or " +
				expectedTokens.get(expectedTokens.size() - 1);
		}
		throw new ParseException(source, peek(), description);
	}

	public void take(int n) {
		if (n < 1) {
			throw new IllegalArgumentException("n must be greater than 0");
		}

		index = Math.min(tokens.size(), index + n);
		expectedTokensAtIndex.clear();
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

	public Tokens expecting(String token) {
		expectedTokensAtIndex.add(token);
		return this;
	}

	public Tokens expectingKeyword(Keyword keyword) {
		return expecting("'" + keyword.getName() + "'");
	}

	public Tokens expectingOperator(Operator operator) {
		return expecting("'" + operator.getName() + "'");
	}

	public Tokens expectingIdent() {
		return expecting("identifier");
	}

	public Tokens expectingIntegerLiteral() {
		return expecting("integer literal");
	}

	public Tokens expectingEof() {
		return expecting("EOF");
	}

	public void expectKeyword(Keyword keyword) throws ParseException {
		Token token = expectingKeyword(keyword).peek();
		if (token.isKeyword(keyword)) {
			take();
		} else {
			error();
		}
	}

	public void expectOperator(Operator operator) throws ParseException {
		Token token = expectingOperator(operator).peek();
		if (token.isOperator(operator)) {
			take();
		} else {
			error();
		}
	}

	public IdentToken expectIdent() throws ParseException {
		Optional<IdentToken> identToken = expecting("identifier").peek().asIdentToken();
		if (identToken.isPresent()) {
			take();
			return identToken.get();
		} else {
			return error();
		}
	}

	public void expectEof() throws ParseException {
		if (!expectingEof().peek().isEof()) {
			error();
		}
	}
}
