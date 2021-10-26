package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.HasSourceSpan;

public sealed interface Token extends HasSourceSpan
	permits WhitespaceToken, CommentToken, KeywordToken, OperatorToken, IdentToken, IntegerLiteralToken, EofToken {
}
