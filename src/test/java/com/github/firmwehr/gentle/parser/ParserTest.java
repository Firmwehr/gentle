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
import com.github.firmwehr.gentle.parser.ast.blockstatement.JustAStatement;
import com.github.firmwehr.gentle.parser.ast.blockstatement.LocalVariableDeclarationStatement;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperatorExpression;
import com.github.firmwehr.gentle.parser.ast.expression.PostfixExpression;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.FieldAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.MethodInvocationOp;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.IdentExpression;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.IntegerLiteralExpression;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.LocalMethodCallExpression;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.NewObjectExpression;
import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.statement.ExpressionStatement;
import com.github.firmwehr.gentle.parser.ast.statement.IfStatement;
import com.github.firmwehr.gentle.parser.ast.statement.ReturnStatement;
import com.github.firmwehr.gentle.parser.ast.type.ArrayType;
import com.github.firmwehr.gentle.parser.ast.type.BooleanType;
import com.github.firmwehr.gentle.parser.ast.type.IdentType;
import com.github.firmwehr.gentle.parser.ast.type.IntType;
import com.github.firmwehr.gentle.parser.ast.type.VoidType;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.Source;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
					new Field(new ArrayType(new IntType()), new Ident("numbers"))
				),
				List.of(
					new Method(
						new VoidType(),
						new Ident("eat"),
						List.of(
							new Parameter(new ArrayType(new IntType()), new Ident("types")),
							new Parameter(new IntType(), new Ident("amount")),
							new Parameter(new BooleanType(), new Ident("raw"))
						),
						new Block(List.of())
					)
				),
				List.of(
					new MainMethod(
						new Ident("main"),
						new Parameter(
							new IdentType(new Ident("String")),
							new Ident("args")
						),
						new Block(List.of())
					)
				)
			),
			new ClassDeclaration(
				new Ident("Bar"),
				List.of(
					new Field(new ArrayType(new ArrayType(new BooleanType())), new Ident("bitmap")),
					new Field(new IdentType(new Ident("Foo")), new Ident("foo"))
				),
				List.of(
					new Method(
						new IdentType(new Ident("Foo")),
						new Ident("getSingleFoo"),
						List.of(),
						new Block(List.of())
					),
					new Method(
						new ArrayType(new IdentType(new Ident("Foo"))),
						new Ident("getManyFoos"),
						List.of(new Parameter(new IntType(), new Ident("amount"))),
						new Block(List.of())
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
				new VoidType(),
				new Ident("add"),
				List.of(new Parameter(new IntType(), new Ident("a")), new Parameter(new IntType(), new Ident("b"))),
				new Block(List.of(
					new JustAStatement(new ReturnStatement(Optional.of(
						new BinaryOperatorExpression(
							new PostfixExpression(new IdentExpression(new Ident("a")), List.of()),
							new PostfixExpression(new IdentExpression(new Ident("b")), List.of()),
							BinaryOperator.ADDITION
						)
					)))
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
				new Method(new VoidType(), new Ident("bar"), List.of(), new Block(List.of(
					new JustAStatement(new ExpressionStatement(
						new BinaryOperatorExpression(
							new BinaryOperatorExpression(
								new PostfixExpression(new IntegerLiteralExpression(2), List.of()),
								new BinaryOperatorExpression(
									new PostfixExpression(new IntegerLiteralExpression(3), List.of()),
									new PostfixExpression(new IntegerLiteralExpression(4), List.of()),
									BinaryOperator.MULTIPLICATION
								),
								BinaryOperator.ADDITION
							),
							new PostfixExpression(new IntegerLiteralExpression(5), List.of()),
							BinaryOperator.ADDITION
						)
					))
				)))
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
				new Method(new VoidType(), new Ident("bar"), List.of(), new Block(List.of(
					new JustAStatement(new ExpressionStatement(
						new BinaryOperatorExpression(
							new BinaryOperatorExpression(
								new BinaryOperatorExpression(
									new PostfixExpression(new IntegerLiteralExpression(5), List.of()),
									new PostfixExpression(new IntegerLiteralExpression(8), List.of()),
									BinaryOperator.MULTIPLICATION
								),
								new PostfixExpression(new IntegerLiteralExpression(9), List.of()),
								BinaryOperator.GREATER_THAN_OR_EQUAL
							),
							new BinaryOperatorExpression(
								new BinaryOperatorExpression(
									new BinaryOperatorExpression(
										new BinaryOperatorExpression(
											new PostfixExpression(new IntegerLiteralExpression(6), List.of()),
											new PostfixExpression(new IntegerLiteralExpression(8), List.of()),
											BinaryOperator.ADDITION
										),
										new PostfixExpression(new IntegerLiteralExpression(7), List.of()),
										BinaryOperator.SUBTRACTION
									),
									new BinaryOperatorExpression(
										new PostfixExpression(new IntegerLiteralExpression(9), List.of()),
										new PostfixExpression(new IntegerLiteralExpression(2), List.of()),
										BinaryOperator.MULTIPLICATION
									),
									BinaryOperator.ADDITION
								),
								new PostfixExpression(new IntegerLiteralExpression(8), List.of()),
								BinaryOperator.LESS_THAN
							),
							BinaryOperator.INEQUALITY
						)
					))
				)))
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
					new IntType(),
					new Ident("fac"),
					List.of(new Parameter(new IntType(), new Ident("n"))),
					new Block(List.of(
						new JustAStatement(new IfStatement(
							new BinaryOperatorExpression(
								new PostfixExpression(new IdentExpression(new Ident("n")), List.of()),
								new PostfixExpression(new IntegerLiteralExpression(2), List.of()),
								BinaryOperator.LESS_THAN
							),
							new ReturnStatement(Optional.of(
								new PostfixExpression(new IntegerLiteralExpression(1), List.of())
							)),
							Optional.empty()
						)),
						new JustAStatement(new ReturnStatement(Optional.of(
							new BinaryOperatorExpression(
								new PostfixExpression(new IdentExpression(new Ident("n")), List.of()),
								new PostfixExpression(new LocalMethodCallExpression(
									new Ident("fac"),
									List.of(new BinaryOperatorExpression(
										new PostfixExpression(new IdentExpression(new Ident("n")), List.of()),
										new PostfixExpression(new IntegerLiteralExpression(1), List.of()),
										BinaryOperator.SUBTRACTION
									))
								), List.of()),
								BinaryOperator.MULTIPLICATION
							)
						)))
					))
				)
			), List.of()),
			new ClassDeclaration(new Ident("Prog3"), List.of(), List.of(), List.of(
				new MainMethod(
					new Ident("main"),
					new Parameter(
						new IdentType(new Ident("String")),
						new Ident("args")
					),
					new Block(List.of(
						new LocalVariableDeclarationStatement(
							new IdentType(new Ident("Factorial")),
							new Ident("f"),
							Optional.of(new PostfixExpression(
								new NewObjectExpression(new Ident("Factorial")),
								List.of()))
						),
						new LocalVariableDeclarationStatement(
							new IntType(),
							new Ident("n"),
							Optional.of(new PostfixExpression(
								new IdentExpression(new Ident("f")),
								List.of(new MethodInvocationOp(
									new Ident("fac"),
									List.of(new PostfixExpression(new IntegerLiteralExpression(42), List.of()))
								))
							))
						),
						new JustAStatement(new ExpressionStatement(new PostfixExpression(
							new IdentExpression(new Ident("System")),
							List.of(
								new FieldAccessOp(new Ident("out")),
								new MethodInvocationOp(
									new Ident("println"),
									List.of(new PostfixExpression(new IdentExpression(new Ident("n")), List.of()))
								)
							)
						)))
					))
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
