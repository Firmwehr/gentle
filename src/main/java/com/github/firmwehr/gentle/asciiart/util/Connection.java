package com.github.firmwehr.gentle.asciiart.util;

import com.github.firmwehr.gentle.asciiart.elements.AsciiElement;

public record Connection(
	AsciiElement start,
	Point startPoint,
	AsciiElement end,
	Point endPoint
) {
}
