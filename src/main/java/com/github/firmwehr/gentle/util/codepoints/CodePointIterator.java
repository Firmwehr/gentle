package com.github.firmwehr.gentle.util.codepoints;

import java.util.PrimitiveIterator;

public interface CodePointIterator extends PrimitiveIterator.OfInt {

	static CodePointIterator iterate(String string, int offset) {
		return new StringCodePointIterator(string, offset);
	}

	static CodePointIterator iterate(String string) {
		return iterate(string, 0);
	}

	/**
	 * Returns the next code point without advancing the iterator.
	 *
	 * @return the next code point.
	 */
	int peekNext();
}
