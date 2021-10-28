package com.github.firmwehr.gentle.util.codepoints;

import java.util.NoSuchElementException;

class StringCodePointIterator implements CodePointIterator {
	private final String string;
	private int nextIndex;

	StringCodePointIterator(String string, int offset) {
		this.string = string;
		this.nextIndex = offset;
	}

	@Override
	public int nextInt() {
		if (!hasNext()) {
			throw new NoSuchElementException("no element remaining");
		}
		return getNextCodePoint(true);
	}

	@Override
	public boolean hasNext() {
		return this.nextIndex < this.string.length();
	}

	@Override
	public int peekNext() {
		if (!hasNext()) {
			throw new NoSuchElementException("no element to peek");
		}
		return getNextCodePoint(false);
	}

	@Override
	public int nextIndex() {
		return this.nextIndex;
	}

	private int getNextCodePoint(boolean increment) {
		int tempIndex = this.nextIndex;
		char first = this.string.charAt(tempIndex);
		int cp = first; // first is returned if we don't come across a valid 2 char codepoint now
		tempIndex++; // first char was read
		if (Character.isHighSurrogate(first)) {
			if (hasNext()) {
				char lower = this.string.charAt(tempIndex);
				// we need to make sure that the lower char is actually a low surrogate
				// otherwise its just broken unicode, but we want to keep the index
				if (Character.isLowSurrogate(lower)) {
					tempIndex++; // our codepoint has 2 chars
					cp = Character.toCodePoint(first, lower);
				}
			}
		}
		if (increment) {
			this.nextIndex = tempIndex; // increment by either 1 or 2
		}
		return cp;
	}
}
