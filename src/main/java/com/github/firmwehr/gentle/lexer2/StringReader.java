package com.github.firmwehr.gentle.lexer2;

import java.util.function.Predicate;

/**
 * A utility reader for a string.
 */
public class StringReader {

	private final String underlying;
	private int position;

	/**
	 * Creates a new string reader.
	 *
	 * @param underlying the underlying string
	 */
	public StringReader(String underlying) {
		this(underlying, 0);
	}

	/**
	 * Creates a new string reader.
	 *
	 * @param underlying the underlying string
	 * @param position the initial position
	 */
	public StringReader(String underlying, int position) {
		this.underlying = underlying;
		this.position = position;
	}

	public boolean canRead() {
		return position < underlying.length();
	}

	/**
	 * Returns true if there is enough input to read {@code amount} chars.
	 *
	 * @param amount the amount of chars to read
	 *
	 * @return true if there is more to read
	 */
	public boolean canRead(int amount) {
		return position + amount <= underlying.length();
	}

	/**
	 * Peeks at a single char.
	 *
	 * @return the char or 0 if EOF is reached
	 */
	public char peek() {
		if (position >= underlying.length()) {
			return 0;
		}
		return underlying.charAt(position);
	}

	public void assertRead(String string) throws LexException {
		if (!readChars(string.length()).equals(string)) {
			throw new LexException("Expected '" + string + "'", this);
		}
	}

	/**
	 * Returns the next {@code amount} chars or less, if the input ends before it
	 *
	 * @param amount the amount of chars to peek at
	 *
	 * @return the read text
	 */
	public String peek(int amount) {
		return underlying.substring(position, Math.min(underlying.length(), position + amount));
	}

	public char readChar() {
		return underlying.charAt(position++);
	}

	public String readChars(int count) {
		int oldPos = this.position;
		position += count;

		return underlying.substring(oldPos, position);
	}

	/**
	 * Reads for as long as {@link #canRead()} is true and the predicate matches.
	 * <p>
	 * Will place the cursor at the first char that did not match.
	 *
	 * @param predicate the predicate
	 *
	 * @return the read string
	 */
	public String readWhile(Predicate<Character> predicate) {
		int start = position;
		while (canRead() && predicate.test(peek())) {
			readChar();
		}

		return underlying.substring(start, position);
	}

	public String readWhitespace() {
		int start = position;
		while (canRead() && Character.isWhitespace(peek())) {
			readChar();
		}

		return underlying.substring(start, position);
	}

	public String assertReadUntil(String needle) throws LexException {
		int needleIndex = underlying.indexOf(needle, position);

		if (needleIndex < 0) {
			throw new LexException("Expected " + needle, this);
		}

		int untilNeedle = needleIndex - position;
		return readChars(untilNeedle + needle.length());
	}

	public String getUnderlying() {
		return underlying;
	}

	public int getPosition() {
		return position;
	}
}
