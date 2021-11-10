package com.github.firmwehr.gentle.source;

/**
 * A contiguous section of the source code file that extends from the character at {@code startOffset} to the character
 * at {@code endOffset - 1}.
 *
 * @param startOffset the start offset of the span (inclusive)
 * @param endOffset the end offset of the span (exclusive)
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

	public static SourceSpan dummy() {
		return new SourceSpan(0, 0);
	}
}
