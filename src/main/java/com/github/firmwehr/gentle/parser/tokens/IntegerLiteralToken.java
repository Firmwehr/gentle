package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.SourceSpan;

public record IntegerLiteralToken(
	SourceSpan sourceSpan,
	int value
) implements Token {
}
