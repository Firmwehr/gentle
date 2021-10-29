package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

import java.math.BigInteger;

public record IntegerLiteralToken(
	SourceSpan sourceSpan,
	BigInteger value
) implements Token {
	@Override
	public String format() {
		// this is NOT debug output, we actually have to rely on the toString output representation
		return "integer literal " + value;
	}
}
