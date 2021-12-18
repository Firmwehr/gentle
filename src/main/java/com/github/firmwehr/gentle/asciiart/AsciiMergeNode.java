package com.github.firmwehr.gentle.asciiart;

import java.util.List;
import java.util.Objects;

public final class AsciiMergeNode implements AsciiElement {
	private final List<Connection> in;
	private final Point location;
	private Connection out;

	AsciiMergeNode(List<Connection> in, Point location) {
		this.in = in;
		this.location = location;
	}

	@Override
	public boolean contains(Point point) {
		return point.equals(location);
	}

	void setOut(Connection out) {
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
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		var that = (AsciiMergeNode) obj;
		return Objects.equals(this.in, that.in) && Objects.equals(this.out, that.out) &&
			Objects.equals(this.location, that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(in, out, location);
	}

	@Override
	public String toString() {
		return "AsciiMergeNode[" + "in=" + in.stream().map(it -> it.start().location()).toList() + ", " + "out=" +
			out.end().location() + ", " + "location=" + location + ']';
	}


}
