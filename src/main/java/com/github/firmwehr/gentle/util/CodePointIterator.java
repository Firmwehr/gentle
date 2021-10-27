package com.github.firmwehr.gentle.util;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

public class CodePointIterator implements PrimitiveIterator.OfInt {
	private final String string;
	private int currentIndex;

	public static CodePointIterator iterate(String string, int offset) {
		return new CodePointIterator(string, offset);
	}

	public static CodePointIterator iterate(String string) {
		return iterate(string, 0);
	}

	private CodePointIterator(String string, int offset) {
		this.string = string;
		this.currentIndex = offset;
	}

	@Override
	public int nextInt() {
		if (!hasNext()) {
			throw new NoSuchElementException("no element remaining");
		}
		return this.string.codePointAt(this.currentIndex++);
	}

	@Override
	public boolean hasNext() {
		return this.currentIndex < this.string.length();
	}

	/**
	 * Returns the next code point without advancing the iterator.
	 *
	 * @return the next code point.
	 */
	public int peekNext() {
		if (!hasNext()) {
			throw new NoSuchElementException("no element to peek");
		}
		return this.string.codePointAt(this.currentIndex);
	}
}
