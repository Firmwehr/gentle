package com.github.firmwehr.gentle.source;

import java.util.Objects;

public class Source {

	private final String content;

	public Source(String content) throws SourceException {
		if (!content.codePoints().allMatch(c -> c <= 127)) {
			throw new SourceException("input contains non-ASCII characters");
		}

		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public String formatErrorAtPosition(int position, String message, String description) {
		int lineStart = position - 1;
		int column = 1;
		while (lineStart > 0) {
			if (content.charAt(lineStart) == '\n' || content.charAt(lineStart) == '\r') {
				lineStart++;
				column--;
				break;
			}
			lineStart--;
			column++;
		}
		int endOfLine = content.length();
		if (content.indexOf('\n', lineStart) >= 0) {
			endOfLine = Math.min(endOfLine, content.indexOf('\n', lineStart));
		}
		if (content.indexOf('\r', lineStart) >= 0) {
			endOfLine = Math.min(endOfLine, content.indexOf('\r', lineStart));
		}
		String line = content.substring(lineStart, endOfLine);

		int lineNumber = 1 + (int) content.substring(0, position)
			.replace("\r\n", "\n")
			.replace("\r", "\n")
			.codePoints()
			.filter(it -> it == '\n')
			.count();

		StringBuilder builder = new StringBuilder();
		builder.append(message)
			.append(" at line ")
			.append(lineNumber)
			.append(":")
			.append(column)
			.append("\n#\n# ")
			.append(line)
			.append("\n# ");

		line.chars().limit(column - 1).map(it -> {
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
