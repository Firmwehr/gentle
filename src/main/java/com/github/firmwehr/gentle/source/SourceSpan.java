package com.github.firmwehr.gentle.source;

/**
 * A contiguous section of the source code file.
 *
 * @param start the first character of the span (inclusive)
 * @param end the last character of the span (inclusive)
 */
public record SourceSpan(
	SourcePosition start,
	SourcePosition end
) {

	public SourceSpan {
		if (start.offset() > end.offset()) {
			throw new IllegalArgumentException("start must not be later than end");
		}
	}

	public String format() {
		return "%s..%s".formatted(start.format(), end.format());
	}
}
