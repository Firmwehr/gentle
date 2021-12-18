package com.github.firmwehr.gentle.asciiart.elements;

import com.github.firmwehr.gentle.asciiart.util.Point;

public sealed interface AsciiElement permits AsciiBox, AsciiMergeNode {
	Point location();

	boolean contains(Point point);
}
