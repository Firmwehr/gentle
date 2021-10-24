package com.github.firmwehr.gentle.source;

/**
 * The position of a single character in the source code file.
 *
 * @param offset total offset in characters from the start of the file (the first character has offset 0)
 * @param line line the character is in (starting with 0)
 * @param column column the character is in (starting with 0). Newlines and carriage returns are counted as the last
 * 	characters of their previous line.
 */
public record SourcePosition(
	int offset,
	int line,
	int column
) {

	public SourcePosition {
		if (offset < 0) {
			throw new IllegalArgumentException("offset must not be negative");
		}
		if (line < 0) {
			throw new IllegalArgumentException("line must not be negative");
		}
		if (column < 0) {
			throw new IllegalArgumentException("column must not be negative");
		}
	}

	public String format() {
		return "%d:%d".formatted(line + 1, column + 1);
	}
}
