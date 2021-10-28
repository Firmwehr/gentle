package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.MainMethod;
import com.github.firmwehr.gentle.parser.ast.Method;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.Source;
import org.junit.jupiter.api.Test;

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
		Program target = new Program().withDecl(new ClassDeclaration("Empty"));

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
		// @formatter:on

		Program output = parser.parse();
		Program target = new Program().withDecl(new ClassDeclaration("Nothing"))
			.withDecl(new ClassDeclaration("to"))
			.withDecl(new ClassDeclaration("c"))
			.withDecl(new ClassDeclaration("HERE"));

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
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("eat")
					.withParam(Type.newInt().atLevel(1), "types")
					.withParam(Type.newInt(), "amount")
					.withParam(Type.newBool(), "raw"))
				.withField(Type.newInt().atLevel(1), "numbers")
				.withMainMethod(new MainMethod("main", Type.newIdent("String"), "args")))
			.withDecl(new ClassDeclaration("Bar")
				.withMethod(new Method("getSingleFoo")
					.returning(Type.newIdent("Foo")))
				.withMethod(new Method("getManyFoos")
					.returning(Type.newIdent("Foo").atLevel(1))
					.withParam(Type.newInt(), "amount"))
				.withField(Type.newBool().atLevel(2), "bitmap")
				.withField(Type.newIdent("Foo"), "foo"));
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
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("add")
					.withParam(Type.newInt(), "a")
					.withParam(Type.newInt(), "b")
					.withBody(Statement.newBlock()
						.thenReturn(Expression.newBinOp(
							Expression.newIdent("a"),
							Expression.newIdent("b"),
							BinaryOperator.ADDITION
						)))));
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
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("bar")
					.withBody(Statement.newBlock()
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
						)))));
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
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("bar")
					.withBody(Statement.newBlock()
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
						)))));
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
		Program target = new Program()
			.withDecl(new ClassDeclaration("Factorial")
				.withMethod(new Method("fac")
					.returning(Type.newInt())
					.withParam(Type.newInt(), "n")
					.withBody(Statement.newBlock()
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
						)))))
			.withDecl(new ClassDeclaration("Prog3")
				.withMainMethod(new MainMethod("main", Type.newIdent("String"), "args")
					.withBody(Statement.newBlock()
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
							.withCall("println", Expression.newIdent("n"))))));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}
}
