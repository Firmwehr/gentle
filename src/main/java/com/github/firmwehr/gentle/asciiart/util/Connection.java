package com.github.firmwehr.gentle.asciiart.util;

import com.github.firmwehr.gentle.asciiart.elements.AsciiElement;

import java.util.Objects;

public record Connection(
	AsciiElement start,
	Point startPoint,
	AsciiElement end,
	Point endPoint
) {

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Connection that = (Connection) o;
		return Objects.equals(startPoint, that.startPoint) && Objects.equals(endPoint, that.endPoint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(startPoint, endPoint);
	}
}
