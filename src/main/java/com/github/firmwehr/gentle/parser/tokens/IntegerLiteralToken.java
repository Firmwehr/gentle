package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

import java.math.BigInteger;

public record IntegerLiteralToken(
	SourceSpan sourceSpan,
	BigInteger value
) implements Token {
	@Override
	public String format() {
		return "integer literal " + value;
	}
}
