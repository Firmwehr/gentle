package com.github.firmwehr.gentle.asciiart;

public record Connection(
	AsciiElement start,
	Point startPoint,
	AsciiElement end,
	Point endPoint
) {
}
