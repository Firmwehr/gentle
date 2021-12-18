package com.github.firmwehr.gentle.asciiart;

import java.util.List;

public record AsciiMergeNode(
	List<Connection> in,
	Connection out,
	Point location
) implements AsciiElement {
}
