package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record CommentToken(
	SourceSpan sourceSpan,
	CommentType type,
	String text
) implements Token {
	public enum CommentType {
		LINE,
		BLOCK
	}
}
