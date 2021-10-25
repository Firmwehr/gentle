package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.Token;
import com.github.firmwehr.gentle.source.Source;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Primary class for lexing strings.
 */
public class Lexer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Lexer.class);

	private final Predicate<TokenType> tokenFilter;
	private final Source source;

	private LexReader reader;

	/**
	 * Constructs a new lexer that will generate tokens from the given source.
	 *
	 * @param source The source file to read.
	 */
	public Lexer(Source source) {
		this(source, tokenType -> true);
	}

	/**
	 * Constructs a new lexer that will generate tokens from the given source.
	 *
	 * @param source The source file to read.
	 * @param tokenTypePredicate A predicate that will be used to drop certain tokens from the output stream. Please
	 * 	consider using {@link #tokenFilter(TokenType...)} to construct this predicate. The predicate is not allowed to
	 * 	reject EOF tokens.
	 *
	 * @throws IllegalArgumentException If the provided predicate is also rejecting EOF tokens since this is
	 * 	impossible.
	 */
	public Lexer(Source source, Predicate<TokenType> tokenTypePredicate) {
		// filtering eof token will cause nextToken() to enter endless loop on reaching end of input
		// note that this is just an optimistic check since predicates could be statefull
		Preconditions.checkArgument(tokenTypePredicate.test(TokenType.EOF),
			"tokenTypePredicate is not allowed to drop EOF token");
		this.source = source;
		this.tokenFilter = tokenTypePredicate;
		this.reader = new LexReader(source);
	}

	/**
	 * Helper method for constructing token filtering predicates from a list of tokens.
	 *
	 * @param tokenTypes List of tokens that should be dropped.
	 *
	 * @return Predicate that will drop the given list of tokens.
	 */
	public static Predicate<TokenType> tokenFilter(TokenType... tokenTypes) {
		var set = Set.of(tokenTypes);
		return (tokenType) -> !set.contains(tokenType);
	}

	/**
	 * Parses the next token in the input stream.
	 *
	 * @return The next parsed token.
	 *
	 * @throws LexerException If the next input segment could not be parsed to a valid token.
	 */
	public Token nextToken() throws LexerException {
		while (true) {
			var token = nextTokenInternal();
			if (tokenFilter.test(token.tokenType())) {
				return token;
			} else if (token.tokenType() == TokenType.EOF) {
				throw new AssertionError("lexer predicate just dropped EOF. This is a bug");
			}
		}
	}

	private Token nextTokenInternal() throws LexerException {
		var parse = TokenType.parseNextToken(reader.fork());
		var childReader = parse.reader();
		var token = parse.token();
		var diff = reader.diff(childReader);

		if (diff.isEmpty() && token.tokenType() != TokenType.EOF) { // eof is allowed to read empty token
			throw new Error("parsed token from empty string, this is an error in the code");
		}

		LOGGER.trace("emitting token {} from string slice @ {}: '{}'", token, token.position().format(), diff);
		reader = childReader;
		return token;
	}
}
