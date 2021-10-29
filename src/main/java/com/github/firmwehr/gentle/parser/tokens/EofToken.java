package com.github.firmwehr.gentle.parser.tokens;

import com.github.firmwehr.gentle.source.SourceSpan;

public record EofToken(SourceSpan sourceSpan) implements Token {
	@Override
	public String format() {
		return "end of file";
	}
}
