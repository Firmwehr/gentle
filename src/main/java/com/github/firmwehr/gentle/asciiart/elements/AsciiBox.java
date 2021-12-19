package com.github.firmwehr.gentle.asciiart.elements;

import com.github.firmwehr.gentle.asciiart.util.Connection;
import com.github.firmwehr.gentle.asciiart.util.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record AsciiBox(
	List<String> lines,
	List<Connection> ins,
	List<Connection> outs,
	Point topLeft,
	Point bottomRight
) implements AsciiElement {

	@Override
	public Point location() {
		return topLeft();
	}

	@Override
	public boolean contains(Point point) {
		boolean insideX = point.x() >= topLeft.x() && point.x() <= bottomRight.x();
		boolean insideY = point.y() >= topLeft.y() && point.y() <= bottomRight.y();
		return insideX && insideY;
	}

	public List<Point> edge() {
		List<Point> points = new ArrayList<>();

		for (int y = topLeft.y(); y < bottomRight.y(); y++) {
			points.add(new Point(topLeft.x(), y));
			points.add(new Point(bottomRight.x(), y));
		}

		for (int x = topLeft.x(); x < bottomRight.x(); x++) {
			points.add(new Point(x, topLeft.y()));
			points.add(new Point(x, bottomRight.y()));
		}

		return points;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AsciiBox asciiBox = (AsciiBox) o;
		return Objects.equals(topLeft, asciiBox.topLeft) && Objects.equals(bottomRight, asciiBox.bottomRight);
	}

	@Override
	public int hashCode() {
		return Objects.hash(topLeft, bottomRight);
	}

	@Override
	public String toString() {
		return "AsciiBox{" + "lines=" + lines + ", ins=" + ins.stream().map(c -> c.start().location()).toList() +
			", outs=" + outs.stream().map(c -> c.end().location()).toList() + ", topLeft=" + topLeft + ", " +
			"bottomRight" + "=" + bottomRight + '}';
	}
}
