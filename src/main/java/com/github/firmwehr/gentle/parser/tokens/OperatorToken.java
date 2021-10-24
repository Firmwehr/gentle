package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.SourceSpan;

public record OperatorToken(
	SourceSpan sourceSpan,
	Operator operator
) implements Token {
}
