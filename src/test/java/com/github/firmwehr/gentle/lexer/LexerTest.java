package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.TokenIdentifier;
import com.github.firmwehr.gentle.lexer.tokens.TokenWhitespace;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceException;
import com.github.firmwehr.gentle.source.SourcePosition;
import com.github.firmwehr.gentle.source.SourceSpan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LexerTest {

	@Test
	void testRecognizeWindowsLineBreaks() throws SourceException, LexerException {
		String tokens = "Hello\r\nWorld";
		Lexer lexer = new Lexer(new Source(tokens));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(0, 1, 1), new SourcePosition(4, 1, 5)), "Hello"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenWhitespace(new SourceSpan(new SourcePosition(5, 1, 6), new SourcePosition(6, 1, 7)), "\r\n"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(7, 2, 1), new SourcePosition(11, 2, 5)), "World"));
	}

	@Test
	void testRecognizeMultipleWindowsLineBreaks() throws SourceException, LexerException {
		String tokens = "Hello\r\n\r\nWorld";
		Lexer lexer = new Lexer(new Source(tokens));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(0, 1, 1), new SourcePosition(4, 1, 5)), "Hello"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenWhitespace(new SourceSpan(new SourcePosition(5, 1, 6), new SourcePosition(8, 2, 2)), "\r\n\r\n"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(9, 3, 1), new SourcePosition(13, 3, 5)), "World"));
	}

	@Test
	void testRecognizeLinuxLineBreaks() throws SourceException, LexerException {
		String tokens = "Hello\nWorld";
		Lexer lexer = new Lexer(new Source(tokens));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(0, 1, 1), new SourcePosition(4, 1, 5)), "Hello"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenWhitespace(new SourceSpan(new SourcePosition(5, 1, 6), new SourcePosition(5, 1, 6)), "\n"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(6, 2, 1), new SourcePosition(10, 2, 5)), "World"));
	}

	@Test
	void testRecognizeMultipleLinuxLineBreaks() throws SourceException, LexerException {
		String tokens = "Hello\n\nWorld";
		Lexer lexer = new Lexer(new Source(tokens));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(0, 1, 1), new SourcePosition(4, 1, 5)), "Hello"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenWhitespace(new SourceSpan(new SourcePosition(5, 1, 6), new SourcePosition(6, 2, 1)), "\n\n"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(7, 3, 1), new SourcePosition(11, 3, 5)), "World"));
	}

	@Test
	void testRecognizeMixedLineBreaks() throws SourceException, LexerException {
		String tokens = "Hello\n\r\n\rWorld";
		Lexer lexer = new Lexer(new Source(tokens));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(0, 1, 1), new SourcePosition(4, 1, 5)), "Hello"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenWhitespace(new SourceSpan(new SourcePosition(5, 1, 6), new SourcePosition(8, 3, 1)), "\n\r\n\r"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(9, 4, 1), new SourcePosition(13, 4, 5)), "World"));
	}


}
