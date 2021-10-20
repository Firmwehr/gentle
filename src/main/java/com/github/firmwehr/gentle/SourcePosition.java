package com.github.firmwehr.gentle;

public record SourcePosition(int line, int column) {
	public String print() {
		return "%d:%d".formatted(line, column);
	}
}
