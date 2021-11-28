package com.github.firmwehr.gentle.source;

import com.github.firmwehr.gentle.InternalCompilerException;

import java.util.Arrays;
import java.util.Collection;

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
			throw new InternalCompilerException("start must not be later than end");
		}
	}

	public static SourceSpan from(SourceSpan span, Collection<SourceSpan> spans) {
		int start = span.startOffset;
		int end = span.endOffset;

		for (SourceSpan sourceSpan : spans) {
			start = Math.min(start, sourceSpan.startOffset);
			end = Math.max(end, sourceSpan.endOffset);
		}

		return new SourceSpan(start, end);
	}

	public static SourceSpan from(SourceSpan span, SourceSpan... spans) {
		return from(span, Arrays.asList(spans));
	}

	public static SourceSpan dummy() {
		return new SourceSpan(0, 0);
	}
}
