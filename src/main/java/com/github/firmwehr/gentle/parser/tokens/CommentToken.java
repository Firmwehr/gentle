package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record CommentToken(
	SourceSpan sourceSpan,
	String text
) implements Token {

	@Override
	public String format() {
		return "comment";
	}
}
