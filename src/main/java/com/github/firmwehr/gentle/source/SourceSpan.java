package com.github.firmwehr.gentle.source;

/**
 * A contiguous section of the source code file.
 *
 * @param startOffset the first character of the span (inclusive)
 * @param endOffset the last character of the span (inclusive)
 */
public record SourceSpan(
	int startOffset,
	int endOffset
) {

	public SourceSpan {
		if (startOffset > endOffset) {
			throw new IllegalArgumentException("start must not be later than end");
		}
	}

	public String format() {
		return startOffset + ".." + endOffset;
	}
}
