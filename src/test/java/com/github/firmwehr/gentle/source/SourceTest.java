package com.github.firmwehr.gentle.source;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceTest {

	@Test
	void multipleLinuxLineEndings() {
		Source source = new Source("Hello\n\nWorld");

		assertThat(source.positionAndLineFromOffset(3).first()).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionAndLineFromOffset(3).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(5).first()).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionAndLineFromOffset(5).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(6).first()).isEqualTo(new SourcePosition(6, 2, 1));
		assertThat(source.positionAndLineFromOffset(6).second()).isEqualTo("");

		assertThat(source.positionAndLineFromOffset(7).first()).isEqualTo(new SourcePosition(7, 3, 1));
		assertThat(source.positionAndLineFromOffset(7).second()).isEqualTo("World");
	}

	@Test
	void multipleWindowsLineEndings() {
		Source source = new Source("Hello\r\n\r\nWorld");

		assertThat(source.positionAndLineFromOffset(3).first()).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionAndLineFromOffset(3).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(5).first()).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionAndLineFromOffset(5).second()).isEqualTo("Hello");
		assertThat(source.positionAndLineFromOffset(6).first()).isEqualTo(new SourcePosition(6, 1, 7));
		assertThat(source.positionAndLineFromOffset(6).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(7).first()).isEqualTo(new SourcePosition(7, 2, 1));
		assertThat(source.positionAndLineFromOffset(7).second()).isEqualTo("");
		assertThat(source.positionAndLineFromOffset(8).first()).isEqualTo(new SourcePosition(8, 2, 2));
		assertThat(source.positionAndLineFromOffset(8).second()).isEqualTo("");

		assertThat(source.positionAndLineFromOffset(9).first()).isEqualTo(new SourcePosition(9, 3, 1));
		assertThat(source.positionAndLineFromOffset(9).second()).isEqualTo("World");
	}

	@Test
	void multipleMacLineEndings() {
		Source source = new Source("Hello\r\rWorld");

		assertThat(source.positionAndLineFromOffset(3).first()).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionAndLineFromOffset(3).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(5).first()).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionAndLineFromOffset(5).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(6).first()).isEqualTo(new SourcePosition(6, 2, 1));
		assertThat(source.positionAndLineFromOffset(6).second()).isEqualTo("");

		assertThat(source.positionAndLineFromOffset(7).first()).isEqualTo(new SourcePosition(7, 3, 1));
		assertThat(source.positionAndLineFromOffset(7).second()).isEqualTo("World");
	}

	@Test
	void linuxLineEndings() {
		Source source = new Source("Hello\nWorld");

		assertThat(source.positionAndLineFromOffset(3).first()).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionAndLineFromOffset(3).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(5).first()).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionAndLineFromOffset(5).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(6).first()).isEqualTo(new SourcePosition(6, 2, 1));
		assertThat(source.positionAndLineFromOffset(6).second()).isEqualTo("World");
	}

	@Test
	void windowsLineEndings() {
		Source source = new Source("Hello\r\nWorld");

		assertThat(source.positionAndLineFromOffset(3).first()).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionAndLineFromOffset(3).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(5).first()).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionAndLineFromOffset(5).second()).isEqualTo("Hello");
		assertThat(source.positionAndLineFromOffset(6).first()).isEqualTo(new SourcePosition(6, 1, 7));
		assertThat(source.positionAndLineFromOffset(6).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(7).first()).isEqualTo(new SourcePosition(7, 2, 1));
		assertThat(source.positionAndLineFromOffset(7).second()).isEqualTo("World");
	}

	@Test
	void macLineEndings() {
		Source source = new Source("Hello\rWorld");

		assertThat(source.positionAndLineFromOffset(3).first()).isEqualTo(new SourcePosition(3, 1, 4));
		assertThat(source.positionAndLineFromOffset(3).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(5).first()).isEqualTo(new SourcePosition(5, 1, 6));
		assertThat(source.positionAndLineFromOffset(5).second()).isEqualTo("Hello");

		assertThat(source.positionAndLineFromOffset(6).first()).isEqualTo(new SourcePosition(6, 2, 1));
		assertThat(source.positionAndLineFromOffset(6).second()).isEqualTo("World");
	}
}
