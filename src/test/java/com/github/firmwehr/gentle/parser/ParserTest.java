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
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
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

		Program output = parser.parse();
		Program target =
			new Program(List.of(new ClassDeclaration(new Ident("Empty"), List.of(), List.of(), List.of())));

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void emptyClasses() throws LexerException, ParseException {
		// @formatter:off
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

		Program output = parser.parse();
		Program target = new Program(List.of(
			new ClassDeclaration(new Ident("Nothing"), List.of(), List.of(), List.of()),
			new ClassDeclaration(new Ident("to"), List.of(), List.of(), List.of()),
			new ClassDeclaration(new Ident("c"), List.of(), List.of(), List.of()),
			new ClassDeclaration(new Ident("HERE"), List.of(), List.of(), List.of())
		));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void classesWithMethodsAndFields() throws LexerException, ParseException {
		// @formatter:off
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

		Program output = parser.parse();
		Program target = new Program(List.of(
			new ClassDeclaration(
				new Ident("Foo"),
				List.of(
					new Field(Type.newInt().atLevel(1), new Ident("numbers"))
				),
				List.of(
					new Method(
						Type.newVoid(),
						new Ident("eat"),
						List.of(
							new Parameter(Type.newInt().atLevel(1), new Ident("types")),
							new Parameter(Type.newInt(), new Ident("amount")),
							new Parameter(Type.newBool(), new Ident("raw"))
						),
						Statement.newBlock()
					)
				),
				List.of(
					new MainMethod(
						new Ident("main"),
						new Parameter(Type.newIdent("String"), new Ident("args")),
						Statement.newBlock()
					)
				)
			),
			new ClassDeclaration(
				new Ident("Bar"),
				List.of(
					new Field(Type.newBool().atLevel(2), new Ident("bitmap")),
					new Field(Type.newIdent("Foo"), new Ident("foo"))
				),
				List.of(
					new Method(
						Type.newIdent("Foo"),
						new Ident("getSingleFoo"),
						List.of(),
						Statement.newBlock()
					),
					new Method(
						Type.newIdent("Foo").atLevel(1),
						new Ident("getManyFoos"),
						List.of(new Parameter(Type.newInt(), new Ident("amount"))),
						Statement.newBlock()
					)
				),
				List.of()
			)
		));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void simpleExpressions() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void add(int a, int b) {
					return a + b;
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program(List.of(new ClassDeclaration(
			new Ident("Foo"),
			List.of(),
			List.of(new Method(
				Type.newVoid(),
				new Ident("add"),
				List.of(
					new Parameter(Type.newInt(), new Ident("a")),
					new Parameter(Type.newInt(), new Ident("b"))
				),
				Statement.newBlock()
					.thenReturn(Expression.newBinOp(
						Expression.newIdent("a"),
						Expression.newIdent("b"),
						BinaryOperator.ADDITION
					))
			)),
			List.of()
		)));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void mediumExpression() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void bar() {
					2 + 3 * 4 + 5;
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program(List.of(
			new ClassDeclaration(new Ident("Foo"), List.of(), List.of(
				new Method(
					Type.newVoid(),
					new Ident("bar"),
					List.of(),
					Statement.newBlock()
						.thenExpr(Expression.newBinOp(
							Expression.newBinOp(
								Expression.newInt(2),
								Expression.newBinOp(
									Expression.newInt(3),
									Expression.newInt(4),
									BinaryOperator.MULTIPLICATION
								),
								BinaryOperator.ADDITION
							),
							Expression.newInt(5),
							BinaryOperator.ADDITION
						))
				)
			), List.of())
		));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void complexExpression() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void bar() {
					5*8>=9!=6+8-7+9*2<8;
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program(List.of(
			new ClassDeclaration(new Ident("Foo"), List.of(), List.of(
				new Method(
					Type.newVoid(),
					new Ident("bar"),
					List.of(),
					Statement.newBlock()
						.thenExpr(Expression.newBinOp(
							Expression.newBinOp(
								Expression.newBinOp(
									Expression.newInt(5),
									Expression.newInt(8),
									BinaryOperator.MULTIPLICATION
								),
								Expression.newInt(9),
								BinaryOperator.GREATER_THAN_OR_EQUAL
							),
							Expression.newBinOp(
								Expression.newBinOp(
									Expression.newBinOp(
										Expression.newBinOp(
											Expression.newInt(6),
											Expression.newInt(8),
											BinaryOperator.ADDITION
										),
										Expression.newInt(7),
										BinaryOperator.SUBTRACTION
									),
									Expression.newBinOp(
										Expression.newInt(9),
										Expression.newInt(2),
										BinaryOperator.MULTIPLICATION
									),
									BinaryOperator.ADDITION
								),
								Expression.newInt(8),
								BinaryOperator.LESS_THAN
							),
							BinaryOperator.INEQUALITY
						))
				)
			), List.of())
		));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void factorialProgram() throws LexerException, ParseException {
		// @formatter:off
		Parser parser = fromText("""
			class Factorial {
				public int fac(int n) {
					if (n < 2)
						return 1;
					return n * fac(n-1);
				}
			}
			
			class Prog3 {
				public static void main(String args) {
					Factorial f = new Factorial();
					int n = f.fac(42);
					System.out.println(n);
				}
			}
			""");

		Program output = parser.parse();
		Program target = new Program(List.of(
			new ClassDeclaration(new Ident("Factorial"), List.of(), List.of(
				new Method(
					Type.newInt(),
					new Ident("fac"),
					List.of(new Parameter(Type.newInt(), new Ident("n"))),
					Statement.newBlock()
						.thenIf(
							Expression.newBinOp(
								Expression.newIdent("n"),
								Expression.newInt(2),
								BinaryOperator.LESS_THAN
							),
							Statement.newReturn(Expression.newInt(1))
						)
						.thenReturn(Expression.newBinOp(
							Expression.newIdent("n"),
							Expression.newCall("fac", Expression.newBinOp(
								Expression.newIdent("n"),
								Expression.newInt(1),
								BinaryOperator.SUBTRACTION
							)),
							BinaryOperator.MULTIPLICATION
						))
				)
			), List.of()),
			new ClassDeclaration(new Ident("Prog3"), List.of(), List.of(), List.of(
				new MainMethod(
					new Ident("main"),
					new Parameter(Type.newIdent("String"), new Ident("args")),
					Statement.newBlock()
						.thenLocalVar(
							Type.newIdent("Factorial"),
							"f",
							Expression.newNewObject("Factorial")
						)
						.thenLocalVar(
							Type.newInt(),
							"n",
							Expression.newIdent("f")
								.withCall("fac", Expression.newInt(42))
						)
						.thenExpr(Expression.newIdent("System")
							.withFieldAccess("out")
							.withCall("println", Expression.newIdent("n")))
				)
			))
		));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}
}
