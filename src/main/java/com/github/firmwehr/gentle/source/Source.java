package com.github.firmwehr.gentle.source;

import java.util.List;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.ansi;

public record Source(String content) {
	private static final int TAB_WIDTH = 4;
	public static final String ERROR_COLOR = ansi().fgRed().toString();
	public static final String LINE_COLOR = ansi().fgBlue().toString();

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
			.chars()
			.filter(it -> it == '\n')
			.count();
	}

	private boolean isLineBreakChar(int offset) {
		return content.charAt(offset) == '\n' || content.charAt(offset) == '\r';
	}

	private boolean isWindowsLinebreak(int offset) {
		return content.charAt(offset) == '\r' && content.charAt(offset + 1) == '\n';
	}

	public SourcePosition positionFromOffset(int offset) {
		int lineStart = startOfLine(offset);

		int column = offset - lineStart + 1; // Columns are 1-indexed
		int row = linebreaksUntil(lineStart) + 1; // Rows are 1-indexed

		return new SourcePosition(offset, row, column);
	}

	public String getLine(int row) {
		return content.lines().skip(row - 1).findFirst().orElse("");
	}

	private List<String> getLines(int startRow, int amount) {
		return content.lines().skip(startRow - 1).limit(amount).collect(Collectors.toList());
	}

	public String formatMessageAt(SourceSpan span, String message) {
		SourcePosition start = positionFromOffset(span.startOffset());
		SourcePosition end = positionFromOffset(span.endOffset());

		if (start.line() == end.line()) {
			return formatSingleLineMessage(start.line(), start.column(), end.column(), message);
		} else {
			int amount = end.line() - start.line() + 1;
			return formatMultilineMessage(start.line(), amount, start.column(), end.column(), message);
		}
	}

	private static void appendRespectingTab(StringBuilder builder, char guide, char toAppend) {
		char nonTabToAppend = (toAppend == '\t') ? ' ' : toAppend;

		if (guide == '\t') {
			builder.append(Character.toString(nonTabToAppend).repeat(TAB_WIDTH));
		} else {
			builder.append(nonTabToAppend);
		}
	}

	private String formatSingleLineMessage(int lineNr, int start, int end, String message) {
		String line = getLine(lineNr);

		StringBuilder codeLine = new StringBuilder();
		StringBuilder noteLine = new StringBuilder();

		String lineNrStr = Integer.toString(lineNr);
		codeLine.append(LINE_COLOR).append(lineNrStr).append(" | ").append(ansi().reset());
		noteLine.append(LINE_COLOR).append(" ".repeat(lineNrStr.length())).append(" | ").append(ansi().reset());

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (i == start - 1) {
				codeLine.append(ERROR_COLOR);
				noteLine.append(ERROR_COLOR);
			} else if (i == end - 1) {
				codeLine.append(ansi().reset());
				noteLine.append(ansi().reset());
			}

			appendRespectingTab(codeLine, c, c);

			if (i < start - 1) {
				appendRespectingTab(noteLine, c, ' ');
			} else if (i < end - 1) {
				appendRespectingTab(noteLine, c, '^');
			}
		}

		noteLine.append(" ").append(ERROR_COLOR).append(message);

		return codeLine.append("\n").append(noteLine).toString();
	}

	private String formatMultilineMessage(int startLineNr, int lineAmount, int start, int end, String message) {
		List<String> lines = getLines(startLineNr, lineAmount);
		if (lines.size() < 2) {
			throw new IllegalArgumentException("too few lines");
		}

		int endLineNr = startLineNr + lineAmount - 1;
		int lineNrWidth = Integer.toString(startLineNr + lineAmount - 1).length();

		StringBuilder builder = new StringBuilder();

		// First line
		formatFirstLineOfMultilineMessage(builder, lines.get(0), lineNrWidth, startLineNr, start);
		builder.append("\n");

		// Middle lines
		for (int i = 1; i < lines.size() - 1; i++) {
			int lineNr = startLineNr + i;
			formatMiddleLineOfMultilineMessage(builder, lines.get(i), lineNrWidth, lineNr);
			builder.append("\n");
		}

		// Last line
		formatLastLineOfMultilineMessage(builder, lines.get(lines.size() - 1), lineNrWidth, endLineNr, end);

		builder.append(" ").append(message);

		return builder.toString();
	}

	private void formatFirstLineOfMultilineMessage(
		StringBuilder builder, String line, int lineNrWidth, int startLineNr, int start
	) {
		StringBuilder codeLine = new StringBuilder();
		StringBuilder noteLine = new StringBuilder();

		codeLine.append(LINE_COLOR)
			.append(("%" + lineNrWidth + "d").formatted(startLineNr))
			.append(" |   ")
			.append(ansi().reset());
		noteLine.append(LINE_COLOR).append(" ".repeat(lineNrWidth)).append(" | ").append(ERROR_COLOR).append(",-");

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			boolean highlighting = start - 1 <= i;
			char highlight = highlighting ? '^' : '-';

			if (start - 1 == i) {
				codeLine.append(ERROR_COLOR);
			}

			appendRespectingTab(codeLine, c, c);
			appendRespectingTab(noteLine, c, highlight);
		}

		builder.append(codeLine).append("\n").append(noteLine);
	}

	private void formatMiddleLineOfMultilineMessage(StringBuilder builder, String line, int lineNrWidth, int lineNr) {
		builder.append(LINE_COLOR)
			.append(("%" + lineNrWidth + "d").formatted(lineNr))
			.append(" |")
			.append(ERROR_COLOR)
			.append(" | ");
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			appendRespectingTab(builder, c, c);
		}
	}

	private void formatLastLineOfMultilineMessage(
		StringBuilder builder, String line, int lineNrWidth, int endLineNr, int end
	) {
		StringBuilder codeLine = new StringBuilder();
		StringBuilder noteLine = new StringBuilder();

		codeLine.append(LINE_COLOR)
			.append(("%" + lineNrWidth + "d").formatted(endLineNr))
			.append(" |")
			.append(ERROR_COLOR)
			.append(" | ");
		noteLine.append(LINE_COLOR).append(" ".repeat(lineNrWidth)).append(" |").append(ERROR_COLOR).append(" `-");

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			appendRespectingTab(codeLine, c, c);

			if (i < end - 1) {
				appendRespectingTab(noteLine, c, '^');
			}
		}

		builder.append(codeLine).append("\n").append(noteLine);
	}
}
