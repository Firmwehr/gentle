package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.InternalCompilerException;
import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.tokens.EofToken;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;
import com.github.firmwehr.gentle.parser.tokens.Keyword;
import com.github.firmwehr.gentle.parser.tokens.KeywordToken;
import com.github.firmwehr.gentle.parser.tokens.Operator;
import com.github.firmwehr.gentle.parser.tokens.OperatorToken;
import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.source.Source;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Tokens {
	private final Source source;
	private final List<Token> tokens;
	private final EofToken lastToken;

	private int index;
	private final Set<ExpectedToken> expectedTokensAtIndex;

	public Tokens(Source source, List<Token> tokens, EofToken lastToken) {
		this.source = source;
		this.tokens = tokens;
		this.lastToken = lastToken;

		index = 0;
		expectedTokensAtIndex = EnumSet.noneOf(ExpectedToken.class);
	}

	public static Tokens fromLexer(Source source, Lexer lexer) throws LexerException {
		List<Token> tokens = lexer.lex();
		EofToken eofToken = (EofToken) tokens.get(tokens.size() - 1);
		return new Tokens(source, tokens.subList(0, tokens.size() - 1), eofToken);
	}

	public <T> T error() throws ParseException {
		List<String> expectedTokens =
			expectedTokensAtIndex.stream().map(ExpectedToken::getDescription).sorted().collect(Collectors.toList());

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

	public Token peek(int offset) {
		if (offset < 0) {
			throw new InternalCompilerException("offset must not be negative");
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

	public Tokens expecting(ExpectedToken token) {
		expectedTokensAtIndex.add(token);
		return this;
	}

	public Token take() {
		Token token = peek();

		if (index < tokens.size()) {
			index++;
		}
		expectedTokensAtIndex.clear();

		return token;
	}


	public KeywordToken takeKeyword(Keyword keyword) throws ParseException {
		if (peek() instanceof KeywordToken t && t.isKeyword(keyword)) {
			take();
			return t;
		} else {
			return error();
		}
	}

	public OperatorToken takeOperator(Operator operator) throws ParseException {
		if (peek() instanceof OperatorToken t && t.isOperator(operator)) {
			take();
			return t;
		} else {
			return error();
		}
	}


	public IdentToken takeIdent() throws ParseException {
		Optional<IdentToken> identToken = peek().asIdentToken();
		if (identToken.isPresent()) {
			take();
			return identToken.get();
		} else {
			return error();
		}
	}

	public void takeEof() throws ParseException {
		if (!peek().isEof()) {
			error();
		}
	}
}
