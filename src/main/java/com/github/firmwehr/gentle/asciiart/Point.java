package com.github.firmwehr.gentle.asciiart;

public record Point(
	int x,
	int y
) {

	public Point translate(Point other) {
		return new Point(x + other.x, y + other.y);
	}
}
