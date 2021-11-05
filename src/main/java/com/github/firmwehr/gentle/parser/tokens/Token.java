package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.HasSourceSpan;

import java.util.Optional;

public sealed interface Token extends HasSourceSpan
	permits WhitespaceToken, CommentToken, KeywordToken, OperatorToken, IdentToken, IntegerLiteralToken, EofToken {

	String format();

	default boolean isEof() {
		return this instanceof EofToken;
	}

	default boolean isKeyword(Keyword keyword) {
		return this instanceof KeywordToken t && t.keyword() == keyword;
	}

	default boolean isOperator(Operator operator) {
		return this instanceof OperatorToken t && t.operator() == operator;
	}

	default Optional<IdentToken> asIdentToken() {
		if (this instanceof IdentToken ident) {
			return Optional.of(ident);
		} else {
			return Optional.empty();
		}
	}

	default boolean isIdent() {
		return asIdentToken().isPresent();
	}

	default Optional<IntegerLiteralToken> asIntegerLiteralToken() {
		if (this instanceof IntegerLiteralToken integerLiteral) {
			return Optional.of(integerLiteral);
		} else {
			return Optional.empty();
		}
	}

	default boolean isIntegerLiteral() {
		return asIntegerLiteralToken().isPresent();
	}
}
