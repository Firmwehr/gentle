package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record CommentToken(
	SourceSpan sourceSpan,
	String text,
	Type type
) implements Token {

	public enum Type {
		SINGLE_LINE,
		MULTI_LINE
	}

	@Override
	public String format() {
		return "comment";
	}
}
