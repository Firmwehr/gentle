package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.source.Source;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {
	public static Parser fromText(String text) throws LexerException {
		return Parser.fromLexer(new Lexer(new Source(text), tokenType -> true));
	}

	@Test
	void emptyClass() throws LexerException {
		Parser parser = fromText("class Empty {}");

		assertThat(parser.parse()).isEqualTo(
			new Program(List.of(new ClassDeclaration(new Ident("Empty"), List.of(), List.of(), List.of()))));
	}
}
