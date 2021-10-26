package com.github.firmwehr.gentle.source;

import com.google.common.base.Preconditions;

/**
 * The position of a single character in the source code file.
 *
 * @param offset total offset in characters from the start of the file (the first character has offset 1!)
 * @param line line the character is in (starting with 1!)
 * @param column column the character is in (starting with 1!). Newlines and carriage returns are counted as the last
 * 	characters of their previous line.
 */
public record SourcePosition(
	int offset,
	int line,
	int column
) {

	// we use 1-based indexing
	public SourcePosition {
		Preconditions.checkArgument(offset > 0, "offset must not be negative");
		Preconditions.checkArgument(line > 0, "line must not be smaller than 1");
		Preconditions.checkArgument(column > 0, "column must not be smaller than 1");
	}

	public String format() {
		return line + ":" + column;
	}
}
