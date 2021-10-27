package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record KeywordToken(
	SourceSpan sourceSpan,
	Keyword keyword
) implements Token {
	@Override
	public String format() {
		return "'" + keyword.getName() + "'";
	}
}
