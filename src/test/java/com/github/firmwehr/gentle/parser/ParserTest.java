package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.Field;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.MainMethod;
import com.github.firmwehr.gentle.parser.ast.Method;
import com.github.firmwehr.gentle.parser.ast.Parameter;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.type.ArrayType;
import com.github.firmwehr.gentle.parser.ast.type.BooleanType;
import com.github.firmwehr.gentle.parser.ast.type.IdentType;
import com.github.firmwehr.gentle.parser.ast.type.IntType;
import com.github.firmwehr.gentle.parser.ast.type.VoidType;
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

	@Test
	void classesWithMethodsAndFields() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void eat(int[] types, int amount, boolean raw) {}
				public int[] numbers;
				public static void main(String args) throws Exception {}
			}
						
			class Bar {
				public Foo getSingleFoo() {}
				public Foo[] getManyFoos(int amount) throws TooManyFoosException {}
				public boolean[][] bitmap;
				public Foo foo;
			}
			""");

		assertThat(parser.parse()).isEqualTo(new Program(List.of(

			new ClassDeclaration(new Ident("Foo"),

				List.of(new Field(new ArrayType(new IntType()), new Ident("numbers"))),

				List.of(new Method(new VoidType(), new Ident("eat"),
					List.of(new Parameter(new ArrayType(new IntType()), new Ident("types")),
						new Parameter(new IntType(), new Ident("amount")),
						new Parameter(new BooleanType(), new Ident("raw"))), new Block(List.of()))),

				List.of(new MainMethod(new Ident("main"), new IdentType(new Ident("String")), new Ident("args"),
					new Block(List.of())))

			),

			new ClassDeclaration(new Ident("Bar"),

				List.of(new Field(new ArrayType(new ArrayType(new BooleanType())), new Ident("bitmap")),
					new Field(new IdentType(new Ident("Foo")), new Ident("foo"))),

				List.of(

					new Method(new IdentType(new Ident("Foo")), new Ident("getSingleFoo"), List.of(),
						new Block(List.of())),

					new Method(new ArrayType(new IdentType(new Ident("Foo"))), new Ident("getManyFoos"),
						List.of(new Parameter(new IntType(), new Ident("amount"))), new Block(List.of()))

				),

				List.of()

			)

		)));
	}
}
