package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.SourcePosition;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

/**
 * This class provides methods for parsing Strings. It's important to note that this class is unicode aware and expects the caller to be as well.
 * <p>
 * <strong>PLEASE NOTE:</strong> All methods besides {@code peek*} will advance the reader by the read string. Meaning that whatever has been returned by the method will
 * not be read in subsequent calls <strong>BY ANY MEANS</strong>. This is especially important to consieder when deciding to include or exclude the final codepoints of a
 * sequence-read (for instance the closing quotes of a string).
 * </p>
 */
public class LexReader {
	
	private static final int PRINT_POSITION_BEFORE = 20;
	private static final int PRINT_POSITION_AFTER = 20;
	
	private static final int CODEPOINT_LINE_FEED = "\n".codePointAt(0);
	private static final int CODEPOINT_CARRIGE_RETURN = "\r".codePointAt(0);
	
	private final String input;
	private int index = 0;
	
	private int lineCount = 1;
	private int charCount = 0;
	
	public LexReader(String input) {
		this.input = input;
	}
	
	@Override
	public String toString() {
		return "LexReader{index=%d, lineCount=%d, charCount=%d, current='%s'}".formatted(index, lineCount, charCount, input.substring(index));
	}
	
	// TODO: should be move to a better place since we will need this in many more places
	public String printPosition(@Nullable String message) {
		var before = Math.max(0, index - PRINT_POSITION_BEFORE); // select slice to left
		var after = Math.min(index + PRINT_POSITION_AFTER, input.length() - 1); // select slice to right
		
		// TODO: cut off right part at linebreaks
		
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
	
	/**
	 * @return A duplicated reader with independent cursor meaning that both readers can be advanced without interfering with each other.
	 */
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
	
	/**
	 * Reads until the given predicate matches. If the end of input is reached an exception will be thrown. If you do want to accept on end of input, use {@link
	 * #readUntilOrEndOfFile(IntPredicate, boolean)}.
	 *
	 * @param predicate            Predicate to match on codepoints.
	 * @param includeLastCodepoint {@code true} if the first mismatching codepoint should be included in the returned string.
	 * @return The read string.
	 * @throws LexerException If end of input is reached without matching the predicate.
	 */
	public String readUntil(IntPredicate predicate, boolean includeLastCodepoint) throws LexerException {
		return readUntil(predicate, includeLastCodepoint, false);
	}
	
	/**
	 * Reads until the given predicate matches. If the end of input is reached the entire remaining String is returned. If you don't want to accept on end of input, use
	 * {@link #readUntil(IntPredicate, boolean)}.
	 *
	 * @param predicate            Predicate to match on codepoints.
	 * @param includeLastCodepoint {@code true} if the first mismatching codepoint should be included in the returned string.
	 * @return The read string.
	 * @throws LexerException Is never actually thrown but might be in the future. So just handle this case as if the requested operation could not be performed.
	 */
	public String readUntilOrEndOfFile(IntPredicate predicate, boolean includeLastCodepoint) throws LexerException {
		return readUntil(predicate, includeLastCodepoint, true);
	}
	
	private String readUntil(IntPredicate predicate, boolean includeLastCodepoint, boolean allowEof) throws LexerException {
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
				if (allowEof)
					break; // accept match
				else
					throw new LexerException("end of input reached without matching expected predicated", this);
		}
		var s = sb.toString();
		advanceSourcePosition(s);
		return s;
	}
	
	/**
	 * Reads until the given needle is found in the remaining input stream. An exception is thrown if the needle could not be found.
	 *
	 * @param needle        The string to search for.
	 * @param includeNeedle {@code true} if the needle itself should be appended to the end of the returned string.
	 * @return
	 * @throws LexerException
	 */
	public String readUntil(String needle, boolean includeNeedle) throws LexerException {
		var match = input.indexOf(needle, index);
		if (match == -1)
			throw new LexerException("could not find needle '%s'".formatted(needle), this);
		
		var s = input.substring(index, match) + (includeNeedle ? needle : "");
		advanceSourcePosition(s);
		return s;
	}
	
	/**
	 * Reads the entire next line. A line is considered to be complete if:
	 * <ul>
	 *     <li>end of input is reached and at least one codepoint could be read</li>
	 *     <li>{@code \r\n} or {@code \n} is reached, regardless of the amount of codepoints right before the line break</li>
	 * </ul>
	 * The terminating linebreak will be included in the returned string and the reader will advance to the next line.
	 *
	 * @return The read line.
	 * @throws LexerException If the reader is right in front of the end of input.
	 */
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
	
	/**
	 * Peeks at the next codepoint without advancing the reader.
	 *
	 * @return The next codepoint.
	 * @throws LexerException If end of input has been reached.
	 */
	public int peek() throws LexerException {
		if (!isEndOfInput())
			return input.codePointAt(index);
		throw new LexerException("end of input reached", this);
	}
	
	/**
	 * Codepoint aware peek. Peeks at the next {@code n} codepoints and returns them as a string.
	 *
	 * @param n The number of codepoints to peek at.
	 * @return String consisting of {@code n} codepoints.
	 * @throws LexerException If the remaining input does not contain the required amount of codepoints.
	 */
	public String peek(int n) throws LexerException {
		var cpts = input.substring(index).codePoints().limit(n).toArray();
		if (cpts.length < n) {
			var overrun = n - cpts.length;
			throw new LexerException("peek exceeded end of input by " + overrun + " codepoints", this);
		}
		return new String(cpts, 0, cpts.length);
	}
	
	/**
	 * Expects the reader to be in front of the given needle. An exception is thrown if this assumption is wrong.
	 *
	 * @param needle The needle to expect.
	 * @throws LexerException If the given needle did not match the upcoming input stream.
	 */
	public void expect(String needle) throws LexerException {
		var i = input.indexOf(needle, index) - index;
		if (i != 0) // needle must be at current cursor position
			throw new LexerException("did not match '%s'".formatted(needle), this);
		advanceSourcePosition(needle);
	}
	
	/**
	 * @return {@code true} if the reader is right in front of the end of input.
	 */
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