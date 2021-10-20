package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.SourcePosition;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

public class LexReader {
	
	private static final int PRINT_POSITION_BEFORE = 20;
	private static final int PRINT_POSITION_AFTER = 20;
	
	private static final int CODEPOINT_LINE_FEED = "\n".codePointAt(0);
	private static final int CODEPOINT_CARRIGE_RETURN = "\r".codePointAt(0);
	
	private final String input;
	private int index = 0;
	
	private int lineCount = 0;
	private int charCount = 0;
	
	public LexReader(String input) {
		this.input = input;
	}
	
	@Override
	public String toString() {
		return "LexReader{index=%d, lineCount=%d, charCount=%d, current='%s'}".formatted(index, lineCount, charCount, input.substring(index));
	}
	
	public String printPosition(@Nullable String message) {
		var before = Math.max(0, index - PRINT_POSITION_BEFORE); // select slice to left
		var after = Math.min(index + PRINT_POSITION_AFTER, input.length() - 1); // select slice to right
		
		var slice = input.substring(before, after);
		var pos = position().print();
		return """
					at line %s
						%s
						%s%s
				""".formatted(
				pos,
				slice,
				" ".repeat(index - before),
				"^---- " + message
				);
	}
	
	public LexReader fork() { // deliberately not named clone to not clash with java clone method
		var other = new LexReader(input);
		other.index = index;
		other.lineCount = lineCount;
		other.charCount = charCount;
		return other;
	}
	
	/**
	 * Calculates the difference of both readers as a string between their current positions. Both readers must be operating on the same string.
	 *
	 * @param other Another reader that is operating on the same string as this one.
	 * @return String slice between both indicies.
	 */
	public String diff(LexReader other) {
		// ensure both readers are operating on same string
		Preconditions.checkArgument(input.equals(other.input), "readers do not share same input string");
		
		int first = Math.min(index, other.index);
		int second = Math.max(index, other.index);
		
		return input.substring(first, second);
	}
	
	public SourcePosition position() {
		return new SourcePosition(lineCount, charCount);
	}
	
	public String readUntil(IntPredicate predicate, boolean includeLastCodepoint) throws LexerException {
		return readUntil(predicate, includeLastCodepoint, false);
	}
	
	public String readUntilOrEndOfFile(IntPredicate predicate, boolean includeLastCodepoint) throws LexerException {
		return readUntil(predicate, includeLastCodepoint, true);
	}
	
	private String readUntil(IntPredicate predicate, boolean includeLastCodepoint, boolean allowEOF) throws LexerException {
		var sb = new StringBuilder();
		var it = input.substring(index).codePoints().iterator();
		while (true) {
			var cp = it.nextInt();
			
			if (predicate.test(cp)) {
				// read matching codepoint if requested
				if (includeLastCodepoint)
					sb.appendCodePoint(cp);
				
				break;
			}
			
			// append read codepoint
			sb.appendCodePoint(cp);
			
			// if we reached end without matching predicate, the read can not be completed
			if (!it.hasNext())
				if (allowEOF)
					break; // accept match
				else
					throw new LexerException("end of input reached without matching expected predicated", this);
		}
		var s = sb.toString();
		advanceSourcePosition(s);
		return s;
	}
	
	public String readUntil(String needle, boolean includeNeedle) throws LexerException {
		var match = input.indexOf(needle, index);
		if (match == -1)
			throw new LexerException("could not find needle '%s'".formatted(needle), this);
		
		var s = input.substring(index, match) + (includeNeedle ? needle : "");
		advanceSourcePosition(s);
		return s;
	}
	
	public String readLine() throws LexerException {
		var cpts = input.substring(index).codePoints().toArray();
		
		// capture entire line (including newline)
		int i = 0;
		while (i < cpts.length) {
			var cp = cpts[i];
			i++; // effectively marks cp as read
			
			if (cp == CODEPOINT_CARRIGE_RETURN) {
				// check for additional \n in case we use windows line endings
				if (i + 1 < cpts.length && cpts[i + 1] == CODEPOINT_LINE_FEED)
					i++;
				break;
			}
			if (cp == CODEPOINT_LINE_FEED) {
				break;
			}
		}
		
		/* if last line ends without linebreak, we can still return the entire line
		 * but if the cursor is placed exactly at the end of the input, we instead throw an exception
		 * creating an empty string would conflict with other invariats like being unable to read once isEndOfInput() becomes true
		 */
		if (i == 0)
			throw new LexerException("unable to read line, end of input reached", this);
		
		// assemble captured codepoints
		var s = new String(cpts, 0, i);
		advanceSourcePosition(s);
		return s;
	}
	
	public int peek() throws LexerException {
		if (index < input.length())
			return input.codePointAt(index);
		throw new LexerException("end of input reached", this);
	}
	
	public void expect(String needle) throws LexerException {
		var i = input.indexOf(needle, index) - index;
		if (i != 0) // needle must be at current cursor position
			throw new LexerException("did not match '%s'".formatted(needle), this);
		advanceSourcePosition(needle);
	}
	
	public boolean isEndOfInput() {
		return input.length() <= index;
	}
	
	/**
	 * Adjusts the internal position counter by the given string. This method assumes to be called with the string that was just read by one of our internal methods.
	 *
	 * @param str A substring of the current reader state by which we advanced.
	 */
	@SuppressWarnings("AssignmentToForLoopParameter")
	private void advanceSourcePosition(String str) {
		
		var cpts = str.codePoints().toArray();
		for (int i = 0; i < cpts.length; i++) {
			var cp = cpts[i];
			if (cp == CODEPOINT_CARRIGE_RETURN) {
				// check for additional \n in case we use windows
				if (i + 1 < cpts.length && cpts[i + 1] == CODEPOINT_LINE_FEED)
					i++;
				lineCount++;
				charCount = 0;
			} else if (cp == CODEPOINT_LINE_FEED) {
				lineCount++;
				charCount = 0;
			} else {
				// just a regular character
				charCount++;
			}
		}
		
		// also increment index
		index += str.length();
	}
	
}
