package com.github.firmwehr.gentle.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTest {

	@Test
	void multipleLinuxLineEndings() {
		Source source = new Source("Hello\n\nWorld");

		assertThat(source.positionFromOffset(3)).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionFromOffset(5)).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionFromOffset(6)).isEqualTo(new SourcePosition(6, 2, 1));
		assertThat(source.positionFromOffset(7)).isEqualTo(new SourcePosition(7, 3, 1));

		assertThat(source.getLine(1)).isEqualTo("Hello");
		assertThat(source.getLine(2)).isEqualTo("");
		assertThat(source.getLine(3)).isEqualTo("World");
	}

	@Test
	void multipleWindowsLineEndings() {
		Source source = new Source("Hello\r\n\r\nWorld");

		assertThat(source.positionFromOffset(3)).isEqualTo(new SourcePosition(3, 1, 4));

		assertThat(source.positionFromOffset(5)).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionFromOffset(6)).isEqualTo(new SourcePosition(6, 1, 7));

		assertThat(source.positionFromOffset(7)).isEqualTo(new SourcePosition(7, 2, 1));
		assertThat(source.positionFromOffset(8)).isEqualTo(new SourcePosition(8, 2, 2));

		assertThat(source.positionFromOffset(9)).isEqualTo(new SourcePosition(9, 3, 1));

		assertThat(source.getLine(1)).isEqualTo("Hello");
		assertThat(source.getLine(2)).isEqualTo("");
		assertThat(source.getLine(3)).isEqualTo("World");
	}

	@Test
	void multipleMacLineEndings() {
		Source source = new Source("Hello\r\rWorld");

		assertThat(source.positionFromOffset(3)).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionFromOffset(5)).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionFromOffset(6)).isEqualTo(new SourcePosition(6, 2, 1));
		assertThat(source.positionFromOffset(7)).isEqualTo(new SourcePosition(7, 3, 1));

		assertThat(source.getLine(1)).isEqualTo("Hello");
		assertThat(source.getLine(2)).isEqualTo("");
		assertThat(source.getLine(3)).isEqualTo("World");
	}

	@Test
	void linuxLineEndings() {
		Source source = new Source("Hello\nWorld");

		assertThat(source.positionFromOffset(3)).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionFromOffset(5)).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionFromOffset(6)).isEqualTo(new SourcePosition(6, 2, 1));

		assertThat(source.getLine(1)).isEqualTo("Hello");
		assertThat(source.getLine(2)).isEqualTo("World");
	}

	@Test
	void windowsLineEndings() {
		Source source = new Source("Hello\r\nWorld");

		assertThat(source.positionFromOffset(3)).isEqualTo(new SourcePosition(3, 1, 4));

		assertThat(source.positionFromOffset(5)).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionFromOffset(6)).isEqualTo(new SourcePosition(6, 1, 7));

		assertThat(source.positionFromOffset(7)).isEqualTo(new SourcePosition(7, 2, 1));

		assertThat(source.getLine(1)).isEqualTo("Hello");
		assertThat(source.getLine(2)).isEqualTo("World");
	}

	@Test
	void macLineEndings() {
		Source source = new Source("Hello\rWorld");

		assertThat(source.positionFromOffset(3)).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionFromOffset(5)).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionFromOffset(6)).isEqualTo(new SourcePosition(6, 2, 1));

		assertThat(source.getLine(1)).isEqualTo("Hello");
		assertThat(source.getLine(2)).isEqualTo("World");
	}
}
