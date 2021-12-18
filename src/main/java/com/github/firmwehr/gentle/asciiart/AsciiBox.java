package com.github.firmwehr.gentle.asciiart;

import java.util.ArrayList;
import java.util.List;

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
	public String toString() {
		return "AsciiBox{" + "lines=" + lines + ", ins=" + ins.stream().map(c -> c.start().location()).toList() +
			", outs=" + outs.stream().map(c -> c.end().location()).toList() + ", topLeft=" + topLeft + ", " +
			"bottomRight" + "=" + bottomRight + '}';
	}
}
