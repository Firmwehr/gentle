package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.parser.source.SourceSpan;

public record CommentToken(
	SourceSpan sourceSpan,
	boolean isBlockComment
) implements Token {
}
