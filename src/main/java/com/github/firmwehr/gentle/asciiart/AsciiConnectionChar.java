package com.github.firmwehr.gentle.asciiart;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum AsciiConnectionChar {
	VERTICAL('│', new Point(0, 1), new Point(0, -1)),
	HORIZONTAL('─', new Point(1, 0), new Point(-1, 0)),
	CORNER_TOP_RIGHT('┐', new Point(-1, 0), new Point(0, 1)),
	CORNER_BOT_RIGHT('┘', new Point(-1, 0), new Point(0, -1)),
	CORNER_TOP_LEFT('┌', new Point(1, 0), new Point(0, 1)),
	CORNER_BOT_LEFT('└', new Point(1, 0), new Point(0, -1)),
	T_DOWN('┬', new Point(-1, 0), new Point(1, 0), new Point(0, 1)),
	T_UP('┴', new Point(-1, 0), new Point(1, 0), new Point(0, -1)),
	T_LEFT('┤', new Point(-1, 0), new Point(0, 1), new Point(0, -1)),
	T_RIGHT('├', new Point(1, 0), new Point(0, 1), new Point(0, -1)),
	ARROW_UP('▲', new Point(0, 1)),
	ARROW_DOWN('▼', new Point(0, -1)),
	ARROW_LEFT('◄', new Point(1, 0)),
	ARROW_RIGHT('►', new Point(-1, 0));

	private final char character;
	private final List<Point> neighbours;

	AsciiConnectionChar(char character, Point... neighbours) {
		this.character = character;
		this.neighbours = Arrays.asList(neighbours);
	}

	public char getCharacter() {
		return character;
	}

	public List<Point> getNeighbours() {
		return neighbours;
	}

	public boolean canConnectBoxes() {
		return switch (this) {
			case ARROW_DOWN, ARROW_RIGHT, ARROW_LEFT, ARROW_UP -> true;
			case T_DOWN, T_RIGHT, T_LEFT, T_UP -> true;
			default -> false;
		};
	}

	public boolean isArrow() {
		return switch (this) {
			case ARROW_DOWN, ARROW_RIGHT, ARROW_LEFT, ARROW_UP -> true;
			default -> false;
		};
	}

	public boolean isT() {
		return switch (this) {
			case T_DOWN, T_RIGHT, T_LEFT, T_UP -> true;
			default -> false;
		};
	}

	public static Optional<AsciiConnectionChar> forChar(char character) {
		for (AsciiConnectionChar value : values()) {
			if (value.getCharacter() == character) {
				return Optional.of(value);
			}
		}
		return Optional.empty();
	}
}
