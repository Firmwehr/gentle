package com.github.firmwehr.gentle.asciiart.elements;

import com.github.firmwehr.gentle.asciiart.util.Connection;
import com.github.firmwehr.gentle.asciiart.util.Point;

import java.util.List;
import java.util.Objects;

public final class AsciiMergeNode implements AsciiElement {
	private final List<Connection> in;
	private final Point location;
	private Connection out;

	public AsciiMergeNode(List<Connection> in, Point location) {
		this.in = in;
		this.location = location;
	}

	@Override
	public boolean contains(Point point) {
		return point.equals(location);
	}

	public void setOut(Connection out) {
		this.out = out;
	}

	public List<Connection> in() {
		return in;
	}

	public Connection out() {
		return out;
	}

	public Point location() {
		return location;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AsciiMergeNode that = (AsciiMergeNode) o;
		return Objects.equals(location, that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}

	@Override
	public String toString() {
		return "AsciiMergeNode[" + "in=" + in.stream().map(it -> it.start().location()).toList() + ", " + "out=" +
			out.end().location() + ", " + "location=" + location + ']';
	}


}
