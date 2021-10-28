package com.github.firmwehr.gentle.util.codepoints;

import java.util.NoSuchElementException;

class StringCodePointIterator implements CodePointIterator {
	private final String string;
	private int currentIndex;

	StringCodePointIterator(String string, int offset) {
		this.string = string;
		this.currentIndex = offset;
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
		return this.currentIndex < this.string.length();
	}

	@Override
	public int peekNext() {
		if (!hasNext()) {
			throw new NoSuchElementException("no element to peek");
		}
		return getNextCodePoint(false);
	}

	private int getNextCodePoint(boolean increment) {
		int index = this.currentIndex;
		char first = this.string.charAt(index);
		if (Character.isHighSurrogate(first)) {
			index++;
			if (hasNext()) {
				char lower = this.string.charAt(index);
				if (increment) {
					this.currentIndex = index + 1; // increment by 2
				}
				return Character.toCodePoint(first, lower);
			}
		}
		if (increment) {
			this.currentIndex = index + 1; // increment by 1
		}
		return first;
	}
}
