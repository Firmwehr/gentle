package com.github.firmwehr.gentle.parser.source;

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
}
