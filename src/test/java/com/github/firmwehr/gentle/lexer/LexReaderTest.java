package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourcePosition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class LexReaderTest {

	private static final String TEST_INPUT = """
		This is the first line.
		This is the second line.
		This is another line.
		An here we even have some numbers 123""";

	@Test
	public void simpleReadTest() throws Exception {
		var reader = new LexReader(new Source(TEST_INPUT));
		var reader2 = reader.fork();
		assertThat(reader.readLine()).isEqualTo("This is the first line.\n");
		assertThat(reader.position()).isEqualTo(new SourcePosition(24, 2, 1));

		assertThat(reader.readUntil(".", false)).isEqualTo("This is the second line");
		assertThat(reader.readLine()).isEqualTo(".\n");
		assertThat(reader.readUntil(cp -> cp == "3".codePointAt(0), true)).isEqualTo("""
			This is another line.
			An here we even have some numbers 123""");
		assertThat(reader.isEndOfInput()).isTrue();
		assertThat(reader.diff(reader2)).isEqualTo(TEST_INPUT);
	}

	@Test
	public void testReaderDiff() throws Exception {
		var reader = new LexReader(new Source(TEST_INPUT));
		var reader2 = reader.fork();

		var line = reader.readLine();
		var diff1 = reader.diff(reader2);
		var diff2 = reader2.diff(reader);

		assertThat(line).isEqualTo(diff1);
		assertThat(diff1).isEqualTo(diff2);
	}

	@Test
	void testUnicodeInSingleLineComments() throws LexerException {
		String comment = "//This is a \uD83D\uDCA9\uD83D\uDCA9\uD83D\uDCA9 comment\nX";
		var reader = new LexReader(new Source(comment));
		var reader2 = reader.fork();

		var line = reader.readLine();
		var diff = reader2.diff(reader);
		assertThat(line).isEqualTo("//This is a \uD83D\uDCA9\uD83D\uDCA9\uD83D\uDCA9 comment\n");
		assertThat(line).isEqualTo(diff);
	}
}
