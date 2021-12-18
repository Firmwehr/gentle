package com.github.firmwehr.gentle.asciiart;

public interface AsciiElement {
	Point location();

	boolean contains(Point point);
}
