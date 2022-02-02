package com.github.firmwehr.gentle.source;

import com.github.firmwehr.gentle.InternalCompilerException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.fusesource.jansi.Ansi.ansi;

public record Source(
	String content,
	int[] lineStarts
) {
	private static final int TAB_WIDTH = 4;
	public static final Charset FILE_CHARSET = StandardCharsets.US_ASCII;
	public static final String ERROR_COLOR = ansi().fgRed().toString();
	public static final String LINE_COLOR = ansi().fgBlue().toString();

	public Source(String content) {
		this(content, precomputeLineLookupArray(content));
	}

	public static Source loadFromFile(Path path) throws IOException {
		String content = Files.readString(path, FILE_CHARSET);
		return new Source(content);
	}

	public SourcePosition positionFromOffset(int offset) {
		int index = Arrays.binarySearch(lineStarts, offset);
		if (index < 0) {
			index = -(index + 1);
			index--;
		}
		var lineStartOffset = lineStarts[index];
		return new SourcePosition(offset, index + 1, offset - lineStartOffset + 1);
	}

	public String getLine(int row) {
		return content.lines().skip(row - 1).findFirst().orElse("");
	}

	private List<String> getLines(int startRow, int amount) {
		return Stream.concat(content.lines(), Stream.generate(() -> ""))
			.skip(startRow - 1)
			.limit(amount)
			.collect(Collectors.toList());
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
			throw new InternalCompilerException("too few lines");
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

		builder.append(" ").append(ERROR_COLOR).append(message);

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

		builder.append(codeLine).append("\n").append(noteLine).append(ansi().reset());
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
		builder.append(ansi().reset());
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

			if (end - 1 == i) {
				codeLine.append(ansi().reset());
				noteLine.append(ansi().reset());
			}

			appendRespectingTab(codeLine, c, c);

			if (i < end - 1) {
				appendRespectingTab(noteLine, c, '^');
			}
		}

		builder.append(codeLine).append("\n").append(noteLine);
	}

	private static int[] precomputeLineLookupArray(String content) {
		// build line start offset lookup array (workaround for not having to rewrite entire lexer logic)
		var offset = 0;
		var lineNum = 0;

		List<String> list = content.lines().toList();
		int[] precomputed = new int[list.size()];
		for (String line : list) {

			precomputed[lineNum] = offset;
			offset += line.length();

			if (offset >= content.length()) {
				// end of input (no trailing line break)
				break;
			} else if (content.charAt(offset) == '\n') {
				offset++;
			} else if (content.charAt(offset) == '\r') {
				offset++;

				if (offset >= content.length()) {
					// end of input
					break;
				} else if (content.charAt(offset) == '\n') {
					offset++;
				}
			}

			lineNum++;
		}

		return precomputed;
	}
}
