package com.github.firmwehr.gentle.lexer2;

import java.util.stream.Collectors;

public class LexException extends Exception {

	private final String input;
	private final int position;
	private final String message;

	public LexException(String message, StringReader reader) {
		super(message);
		this.message = message;
		this.input = reader.getUnderlying();
		this.position = reader.getPosition();
	}

	@Override
	public String getMessage() {
		return "\n" + toString();
	}

	@Override
	public String toString() {
		String lineWithError = input;
		int currentOffset = 0;
		int positionInErrorLine = 0;
		for (String line : input.lines().collect(Collectors.toList())) {
			if (position >= currentOffset && position <= line.length() + currentOffset) {
				lineWithError = line;
				positionInErrorLine = position - currentOffset;
				break;
			}
			currentOffset += line.length();
		}

		if (lineWithError.length() > 120) {
			int start = Math.max(0, positionInErrorLine - 120 / 2);
			int end = Math.min(lineWithError.length(), positionInErrorLine + 120 / 2);
			lineWithError = lineWithError.substring(start, end);
			positionInErrorLine -= start;
		}

		String pointerLine = " ".repeat(positionInErrorLine) + "^";
		String errorPadding = " ".repeat(Math.max(0, positionInErrorLine - message.length() / 2));
		String centeredError = errorPadding + message;

		return lineWithError + "\n" + pointerLine + "\n" + centeredError;
	}
}
