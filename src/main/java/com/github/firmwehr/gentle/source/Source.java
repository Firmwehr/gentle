package com.github.firmwehr.gentle.source;

import com.github.firmwehr.gentle.util.Pair;

import java.util.Objects;

public class Source {

	private final String content;

	public Source(String content) throws SourceException {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	/**
	 * Find the beginning of the line a certain character is in. If the character is part of a linebreak, it belongs to
	 * the line preceding the line break.
	 *
	 * @param offset arbitrary character offset (may extend beyond end of content)
	 *
	 * @return index of the line's first character
	 */
	private int startOfLine(int offset) {
		// First, go backwards until the end of the previous line to find the start of the current line.
		int lineStart = offset;

		if (offset >= content.length()) {
			// We were past the end of the content, so we just skip back to the last character of the file. Since we
			// didn't point at any character, we certainly didn't start at a linebreak character, so we can just skip
			// that part of the logic.
			lineStart = content.length() - 1;
		} else if (isLineBreakChar(lineStart)) {
			// Line breaks belong to the preceding line, not the following line.
			// If we start in a line break, we advance to the character preceding the line break.
			lineStart--;
			if (lineStart >= 0 && isWindowsLinebreak(lineStart)) {
				// We started on the \n in a \r\n, so we need to take another step back.
				lineStart--;
			}
		}

		// Now, advance backwards until we find the line break of the previous line.
		while (lineStart >= 0 && !isLineBreakChar(lineStart)) {
			lineStart--;
		}

		// Finally, move forward one step so that we're at the beginning of the current line.
		lineStart++;

		return lineStart;
	}

	/**
	 * @param lineStart index of the line's first character
	 *
	 * @return 1 + index of the line's last non-linebreak character
	 */
	private int endOfLine(int lineStart) {
		int index = content.length();

		int newlineIndex = content.indexOf('\n', lineStart);
		if (newlineIndex >= 0 && newlineIndex < index) {
			index = newlineIndex;
		}

		int carriageReturnIndex = content.indexOf('\r', lineStart);
		if (carriageReturnIndex >= 0 && carriageReturnIndex < index) {
			index = carriageReturnIndex;
		}

		return index;
	}

	/**
	 * @param offset arbitrary but valid character offset
	 *
	 * @return amount of linebreaks before (and excluding) the specified character
	 */
	private int linebreaksUntil(int offset) {
		return (int) content.substring(0, offset)
			.replace("\r\n", "\n")
			.replace("\r", "\n")
			.codePoints()
			.filter(it -> it == '\n')
			.count();
	}

	public Pair<SourcePosition, String> positionAndLineFromOffset(int offset) {
		int lineStart = startOfLine(offset);
		int lineEnd = endOfLine(lineStart);
		String line = content.substring(lineStart, lineEnd);

		int column = offset - lineStart + 1; // Columns are 1-indexed
		int row = linebreaksUntil(lineStart) + 1; // Rows are 1-indexed

		return new Pair<>(new SourcePosition(offset, row, column), line);
	}

	private boolean isLineBreakChar(int offset) {
		return content.charAt(offset) == '\n' || content.charAt(offset) == '\r';
	}

	private boolean isWindowsLinebreak(int offset) {
		return content.charAt(offset) == '\r' && content.charAt(offset + 1) == '\n';
	}

	public String formatErrorAtOffset(int offset, String message, String description) {
		Pair<SourcePosition, String> positionAndLine = positionAndLineFromOffset(offset);
		SourcePosition position = positionAndLine.first();
		String line = positionAndLine.second();

		StringBuilder builder = new StringBuilder();
		builder.append(message)
			.append(" at line ")
			.append(position.line())
			.append(":")
			.append(position.column())
			.append("\n#\n# ")
			.append(line)
			.append("\n# ");

		line.chars().limit(position.column() - 1).map(it -> {
			if (it == ' ' || it == '\t') {
				return it;
			} else {
				return ' ';
			}
		}).forEach(it -> builder.append((char) it));

		builder.append("^ ").append(description);

		return builder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Source source = (Source) o;
		return Objects.equals(content, source.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(content);
	}
}
