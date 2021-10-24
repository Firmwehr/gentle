package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.SourceSpan;

public record KeywordToken(
	SourceSpan sourceSpan,
	Keyword keyword
) implements Token {
}
