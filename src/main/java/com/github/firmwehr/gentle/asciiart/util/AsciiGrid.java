package com.github.firmwehr.gentle.asciiart.util;

import java.util.List;
import java.util.Optional;

public class AsciiGrid {
	private final int width;
	private final int height;
	private final char[][] data;

	private AsciiGrid(int height, int width, char[][] data) {
		this.height = height;
		this.width = width;
		this.data = data;
	}

	public static AsciiGrid fromString(String input) {
		List<String> lines = input.lines().toList();
		int height = lines.size();
		int width = 0;
		char[][] data = new char[height][];

		for (int y = 0; y < lines.size(); y++) {
			String line = lines.get(y);
			width = Math.max(width, line.length());
			data[y] = new char[line.length()];

			for (int x = 0; x < line.length(); x++) {
				data[y][x] = line.charAt(x);
			}
		}

		return new AsciiGrid(height, width, data);
	}

	public char get(Point point) {
		return get(point.x(), point.y());
	}

	public char get(int x, int y) {
		if (x < 0 || y < 0) {
			return ' ';
		}
		if (y >= data.length) {
			return ' ';
		}
		if (x >= data[y].length) {
			return ' ';
		}

		return data[y][x];
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Optional<Point> find(char needle) {
		for (int y = 0; y < getHeight(); y++) {
			for (int x = 0; x < getWidth(); x++) {
				if (get(x, y) == needle) {
					return Optional.of(new Point(x, y));
				}
			}
		}

		return Optional.empty();
	}

	public Optional<Point> findInColumn(Point start, char needle, VerticalDirection direction) {
		for (int y = start.y(); y < getHeight() && y >= 0; y += direction.delta()) {
			char character = get(start.x(), y);

			if (character == needle) {
				return Optional.of(new Point(start.x(), y));
			}
		}

		return Optional.empty();
	}

	public Optional<Point> findInRow(Point start, char needle, HorizontalDirection direction) {
		for (int x = start.x(); x < getWidth() && x >= 0; x += direction.delta()) {
			char character = get(x, start.y());

			if (character == needle) {
				return Optional.of(new Point(x, start.y()));
			}
		}

		return Optional.empty();
	}

	public String readRowUntilCol(Point start, int maxX) {
		StringBuilder line = new StringBuilder();

		for (int x = start.x(); x < maxX; x++) {
			line.append(get(x, start.y()));
		}

		return line.toString();
	}

	public enum VerticalDirection {
		DOWN(1),
		UP(-1);

		private final int delta;

		VerticalDirection(int delta) {
			this.delta = delta;
		}

		public int delta() {
			return delta;
		}
	}


	public enum HorizontalDirection {
		RIGHT(1),
		LEFT(-1);

		private final int delta;

		HorizontalDirection(int delta) {
			this.delta = delta;
		}

		public int delta() {
			return delta;
		}
	}
}
