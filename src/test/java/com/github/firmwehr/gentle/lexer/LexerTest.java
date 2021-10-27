package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.TokenIdentifier;
import com.github.firmwehr.gentle.lexer.tokens.TokenWhitespace;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourcePosition;
import com.github.firmwehr.gentle.source.SourceSpan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LexerTest {

	@Test
	void testRecognizeWindowsLineBreaks() throws LexerException {
		String tokens = "Hello\r\nWorld";
		Lexer lexer = new Lexer(new Source(tokens));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(0, 1, 1), new SourcePosition(5, 1, 5)), "Hello"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenWhitespace(new SourceSpan(new SourcePosition(6, 1, 6), new SourcePosition(8, 1, 8)), "\r\n"));
		assertThat(lexer.nextToken()).isEqualTo(
			new TokenIdentifier(new SourceSpan(new SourcePosition(9, 2, 1), new SourcePosition(14, 2, 5)), "World"));
	}

}