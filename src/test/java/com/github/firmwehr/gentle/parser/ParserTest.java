package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.source.Source;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParserTest {
	public static Parser fromText(String text) throws LexerException {
		Source source = new Source(text);
		return Parser.fromLexer(source, new Lexer(source, Lexer.tokenFilter(TokenType.WHITESPACE, TokenType.COMMENT)));
	}

	@Test
	void emptyClass() throws LexerException, ParseException {
		Parser parser = fromText("class Empty {}");

		assertThat(parser.parse()).isEqualTo(
			new Program(List.of(new ClassDeclaration(new Ident("Empty"), List.of(), List.of(), List.of()))));
	}

	@Test
	void emptyClasses() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Nothing {}
			class to { }
			class    c  {
			}
			   
			class
			HERE
			{
			}
			""");

		assertThat(parser.parse()).isEqualTo(new Program(
			List.of(new ClassDeclaration(new Ident("Nothing"), List.of(), List.of(), List.of()),
				new ClassDeclaration(new Ident("to"), List.of(), List.of(), List.of()),
				new ClassDeclaration(new Ident("c"), List.of(), List.of(), List.of()),
				new ClassDeclaration(new Ident("HERE"), List.of(), List.of(), List.of()))));
	}
}
