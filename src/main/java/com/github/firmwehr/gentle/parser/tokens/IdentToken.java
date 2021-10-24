package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.SourceSpan;

public record IdentToken(
	SourceSpan sourceSpan,
	String ident
) implements Token {
}
