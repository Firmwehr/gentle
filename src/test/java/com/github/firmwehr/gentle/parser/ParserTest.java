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
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;
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
							BinaryOperator.ADD
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
									BinaryOperator.MULTIPLY
								),
								BinaryOperator.ADD
							),
							Expression.newInt(5),
							BinaryOperator.ADD
						)))));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void parenthesisedExpression() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void bar() {
					((2 + 3) * ((4) + 5));
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
								Expression.newInt(3),
								BinaryOperator.ADD
							),
							Expression.newBinOp(
								Expression.newInt(4),
								Expression.newInt(5),
								BinaryOperator.ADD
							),
							BinaryOperator.MULTIPLY
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
									BinaryOperator.MULTIPLY
								),
								Expression.newInt(9),
								BinaryOperator.GREATER_OR_EQUAL
							),
							Expression.newBinOp(
								Expression.newBinOp(
									Expression.newBinOp(
										Expression.newBinOp(
											Expression.newInt(6),
											Expression.newInt(8),
											BinaryOperator.ADD
										),
										Expression.newInt(7),
										BinaryOperator.SUBTRACT
									),
									Expression.newBinOp(
										Expression.newInt(9),
										Expression.newInt(2),
										BinaryOperator.MULTIPLY
									),
									BinaryOperator.ADD
								),
								Expression.newInt(8),
								BinaryOperator.LESS_THAN
							),
							BinaryOperator.NOT_EQUAL
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
								BinaryOperator.SUBTRACT
							)),
							BinaryOperator.MULTIPLY
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

	@Test
	void ifAndWhile() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Math {
				public int clamp(int n, int min, int max) {
					if (n < min)
						return min;
					else if (n > max)
						return max;
					else
						return n;
				}
				
				public int sign(int n) {
					return clamp(n, -1, 1);
				}
				
				public int sum(int n) {
					int total = 0;
					int i = 1;
					while (i <= n) {
						total = total + i;
						i = i + 1;
					}
					return total;
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program()
			.withDecl(new ClassDeclaration("Math")
				.withMethod(new Method("clamp")
					.returning(Type.newInt())
					.withParam(Type.newInt(), "n")
					.withParam(Type.newInt(), "min")
					.withParam(Type.newInt(), "max")
					.withBody(Statement.newBlock()
						.thenIf(
							Expression.newBinOp(
								Expression.newIdent("n"),
								Expression.newIdent("min"),
								BinaryOperator.LESS_THAN
							),
							Statement.newReturn(Expression.newIdent("min")),
							Statement.newIf(
								Expression.newBinOp(
									Expression.newIdent("n"),
									Expression.newIdent("max"),
									BinaryOperator.GREATER_THAN
								),
								Statement.newReturn(Expression.newIdent("max")),
								Statement.newReturn(Expression.newIdent("n"))
							)
						)))
				.withMethod(new Method("sign")
					.returning(Type.newInt())
					.withParam(Type.newInt(), "n")
					.withBody(Statement.newBlock()
						.thenReturn(Expression.newCall(
							"clamp",
							Expression.newIdent("n"),
							Expression.newInt(1).withUnary(UnaryOperator.NEGATION),
							Expression.newInt(1)
						))))
				.withMethod(new Method("sum")
					.returning(Type.newInt())
					.withParam(Type.newInt(), "n")
					.withBody(Statement.newBlock()
						.thenLocalVar(Type.newInt(), "total", Expression.newInt(0))
						.thenLocalVar(Type.newInt(), "i", Expression.newInt(1))
						.thenWhile(
							Expression.newBinOp(
								Expression.newIdent("i"),
								Expression.newIdent("n"),
								BinaryOperator.LESS_OR_EQUAL
							),
							Statement.newBlock()
								.thenExpr(Expression.newBinOp(
									Expression.newIdent("total"),
									Expression.newBinOp(
										Expression.newIdent("total"),
										Expression.newIdent("i"),
										BinaryOperator.ADD
									),
									BinaryOperator.ASSIGN
								))
								.thenExpr(Expression.newBinOp(
									Expression.newIdent("i"),
									Expression.newBinOp(
										Expression.newIdent("i"),
										Expression.newInt(1),
										BinaryOperator.ADD
									),
									BinaryOperator.ASSIGN
								))
						)
						.thenReturn(Expression.newIdent("total")))));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void danglingElse() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void bar() {
					if (a)
						if (b)
							return 0;
						else
							return 1;
					return 2;
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("bar")
					.withBody(Statement.newBlock()
						.thenIf(Expression.newIdent("a"), Statement.newIf(
							Expression.newIdent("b"),
							Statement.newReturn(Expression.newInt(0)),
							Statement.newReturn(Expression.newInt(1))
						))
						.thenReturn(Expression.newInt(2)))));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void nestedBlockStatements() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void bar() {
					int a;
					{
						int b;
						{
							return a + b;
						}
					}
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("bar")
					.withBody(Statement.newBlock()
						.thenLocalVar(Type.newInt(), "a")
						.thenBlock(Statement.newBlock()
							.thenLocalVar(Type.newInt(), "b")
							.thenBlock(Statement.newBlock()
								.thenReturn(Expression.newBinOp(
									Expression.newIdent("a"),
									Expression.newIdent("b"),
									BinaryOperator.ADD
								)))))));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}

	@Test
	void moarSemicolons() throws LexerException, ParseException {
		Parser parser = fromText("""
			class Foo {
				public void bar() {
					;int a = 3;;
					;while (false);;
					;if (true);;
					;if (false);else;;
					;return;;
				}
				
				public void smile() {
					  ;;  ;;
					
					;;      ;;
					  ;;;;;;
				}
			}
			""");

		// @formatter:off
		Program output = parser.parse();
		Program target = new Program()
			.withDecl(new ClassDeclaration("Foo")
				.withMethod(new Method("bar")
					.withBody(Statement.newBlock()
						.thenEmpty()
						.thenLocalVar(Type.newInt(), "a", Expression.newInt(3))
						.thenEmpty()
						.thenEmpty()
						.thenWhile(Expression.newBool(false), Statement.newEmpty())
						.thenEmpty()
						.thenEmpty()
						.thenIf(Expression.newBool(true), Statement.newEmpty())
						.thenEmpty()
						.thenEmpty()
						.thenIf(Expression.newBool(false), Statement.newEmpty(), Statement.newEmpty())
						.thenEmpty()
						.thenEmpty()
						.thenReturn()
						.thenEmpty()))
				.withMethod(new Method("smile")
					.withBody(Statement.newBlock()
						.thenEmpty().thenEmpty().thenEmpty().thenEmpty()
						.thenEmpty().thenEmpty().thenEmpty().thenEmpty()
						.thenEmpty().thenEmpty().thenEmpty().thenEmpty().thenEmpty().thenEmpty())));
		// @formatter:on

		System.out.println(PrettyPrinter.format(output));
		System.out.println();
		System.out.println(PrettyPrinter.format(target));

		assertThat(output).isEqualTo(target);
	}
}
