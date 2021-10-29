package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record IntegerLiteralToken(
	SourceSpan sourceSpan,
	int value
) implements Token {
	@Override
	public String format() {
		return "integer literal " + value;
	}
}
