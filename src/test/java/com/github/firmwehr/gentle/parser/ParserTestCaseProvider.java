package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.parser.ast.ClassDeclaration;
import com.github.firmwehr.gentle.parser.ast.MainMethod;
import com.github.firmwehr.gentle.parser.ast.Method;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class ParserTestCaseProvider implements ArgumentsProvider {
	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext ctx) {
		// @formatter:off
		return Stream.of(
			Arguments.of(new ParserTestCase("empty program",
				"",
				new Program()
			)),
			Arguments.of(new ParserTestCase("empty class",
				"class Empty {}",
				new Program().withDecl(ClassDeclaration.dummy("Empty"))
			)),
			Arguments.of(new ParserTestCase("empty classes",
				"""
				class Nothing {}
				class to { }
				class    c  {
				}
				
				class
				HERE
				{
				}
				""",
				new Program().withDecl(ClassDeclaration.dummy("Nothing"))
					.withDecl(ClassDeclaration.dummy("to"))
					.withDecl(ClassDeclaration.dummy("c"))
					.withDecl(ClassDeclaration.dummy("HERE"))
			)),
			Arguments.of(new ParserTestCase("classes with methods and fields",
				"""
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
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("eat")
							.withParam(Type.newInt().atLevel(1), "types")
							.withParam(Type.newInt(), "amount")
							.withParam(Type.newBool(), "raw"))
						.withField(Type.newInt().atLevel(1), "numbers")
						.withMainMethod(MainMethod.dummy("main", Type.newIdent("String"), "args")))
					.withDecl(ClassDeclaration.dummy("Bar")
						.withMethod(Method.dummy("getSingleFoo")
							.returning(Type.newIdent("Foo")))
						.withMethod(Method.dummy("getManyFoos")
							.returning(Type.newIdent("Foo").atLevel(1))
							.withParam(Type.newInt(), "amount"))
						.withField(Type.newBool().atLevel(2), "bitmap")
						.withField(Type.newIdent("Foo"), "foo"))
			)),
			Arguments.of(new ParserTestCase("simple expression",
				"""
				class Foo {
					public void add(int a, int b) {
						return a + b;
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("add")
							.withParam(Type.newInt(), "a")
							.withParam(Type.newInt(), "b")
							.withBody(Statement.newBlock()
								.thenReturn(Expression.newBinOp(
									Expression.newIdent("a"),
									Expression.newIdent("b"),
									BinaryOperator.ADD
								)))))
			)),
			Arguments.of(new ParserTestCase("medium expression",
				"""
				class Foo {
					public void bar() {
						2 + 3 * 4 + 5;
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
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
								)))))
			)),
			Arguments.of(new ParserTestCase("parenthesised expression",
				"""
				class Foo {
					public void bar() {
						((2 + 3) * ((4) + 5));
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
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
								)))))
			)),
			Arguments.of(new ParserTestCase("complex expression",
				"""
				class Foo {
					public void bar() {
						5*8>=9!=6+8-7+9*2<8;
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
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
								)))))
			)),
			Arguments.of(new ParserTestCase("sum of primary expressions",
				"""
				class SumOfPrimExpr {
					public int foo() {
						return null + false + true + 42 + foo + foo() + foo(42) + foo(42, foo) + this + (6 * 7) + new Bar() + new Bar[100][];
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("SumOfPrimExpr")
						.withMethod(Method.dummy("foo")
							.returning(Type.newInt())
							.withBody(Statement.newBlock()
								.thenReturn(Stream.of(
									Expression.newNull(),
									Expression.newBool(false),
									Expression.newBool(true),
									Expression.newInt(42),
									Expression.newIdent("foo"),
									Expression.newCall("foo"),
									Expression.newCall("foo", Expression.newInt(42)),
									Expression.newCall("foo", Expression.newInt(42), Expression.newIdent("foo")),
									Expression.newThis(),
									Expression.newBinOp(Expression.newInt(6), Expression.newInt(7), BinaryOperator.MULTIPLY),
									Expression.newNewObject("Bar"),
									Expression.newNewArray(Type.newIdent("Bar").atLevel(2), Expression.newInt(100))
									// Sadly there's no foldl1 so the first expression (null) must be given as the neutral element
								).reduce((l, r) -> Expression.newBinOp(l, r, BinaryOperator.ADD)).get()))))
			)),
			Arguments.of(new ParserTestCase("array access expression",
				"""
				class Postfix {
					public int foo() {
						return a[i - j][b[this]];
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Postfix")
						.withMethod(Method.dummy("foo")
							.returning(Type.newInt())
							.withBody(Statement.newBlock()
								.thenReturn(Expression.newIdent("a")
									.withArrayAccess(Expression.newBinOp(Expression.newIdent("i"), Expression.newIdent("j"), BinaryOperator.SUBTRACT))
									.withArrayAccess(Expression.newIdent("b")
										.withArrayAccess(Expression.newThis()))
								))))
			)),
			Arguments.of(new ParserTestCase("recursive factorial program",
				"""
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
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Factorial")
						.withMethod(Method.dummy("fac")
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
					.withDecl(ClassDeclaration.dummy("Prog3")
						.withMainMethod(MainMethod.dummy("main", Type.newIdent("String"), "args")
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
									.withCall("println", Expression.newIdent("n"))))))
			)),
			Arguments.of(new ParserTestCase("if and while",
				"""
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
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Math")
						.withMethod(Method.dummy("clamp")
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
						.withMethod(Method.dummy("sign")
							.returning(Type.newInt())
							.withParam(Type.newInt(), "n")
							.withBody(Statement.newBlock()
								.thenReturn(Expression.newCall(
									"clamp",
									Expression.newIdent("n"),
									Expression.newInt(-1),
									Expression.newInt(1)
								))))
						.withMethod(Method.dummy("sum")
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
								.thenReturn(Expression.newIdent("total")))))
			)),
			Arguments.of(new ParserTestCase("dangling else",
				"""
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
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
							.withBody(Statement.newBlock()
								.thenIf(Expression.newIdent("a"), Statement.newIf(
									Expression.newIdent("b"),
									Statement.newReturn(Expression.newInt(0)),
									Statement.newReturn(Expression.newInt(1))
								))
								.thenReturn(Expression.newInt(2)))))
			)),
			Arguments.of(new ParserTestCase("nested block statements",
				"""
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
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
							.withBody(Statement.newBlock()
								.thenLocalVar(Type.newInt(), "a")
								.thenBlock(Statement.newBlock()
									.thenLocalVar(Type.newInt(), "b")
									.thenBlock(Statement.newBlock()
										.thenReturn(Expression.newBinOp(
											Expression.newIdent("a"),
											Expression.newIdent("b"),
											BinaryOperator.ADD
										)))))))
			)),
			Arguments.of(new ParserTestCase("moar semicolons",
				"""
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
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
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
						.withMethod(Method.dummy("smile")
							.withBody(Statement.newBlock()
								.thenEmpty().thenEmpty().thenEmpty().thenEmpty()
								.thenEmpty().thenEmpty().thenEmpty().thenEmpty()
								.thenEmpty().thenEmpty().thenEmpty().thenEmpty().thenEmpty().thenEmpty())))
			)),
			Arguments.of(new ParserTestCase(
				"array access on a NewArrayExpression",
				"""
				class Foo {
					public void bar() {
						new int[1][2];
						new Foo[3][][4];
						new void[5][][][];
					}
				}
				""",
				new Program()
					.withDecl(ClassDeclaration.dummy("Foo")
						.withMethod(Method.dummy("bar")
							.withBody(Statement.newBlock()
								.thenExpr(Expression.newNewArray(
									Type.newInt().atLevel(1),
									Expression.newInt(1)
								).withArrayAccess(Expression.newInt(2)))
								.thenExpr(Expression.newNewArray(
									Type.newIdent("Foo").atLevel(2),
									Expression.newInt(3)
								).withArrayAccess(Expression.newInt(4)))
								.thenExpr(Expression.newNewArray(
									Type.newVoid().atLevel(4),
									Expression.newInt(5)
								))))))),
		Arguments.of(new ParserTestCase(
			"integer literal negations",
			"""
			class Foo {
				public void bar() {
					-1;
					-(1);
					1.foo();
					-1.foo();
					(-1).foo();
					1[2];
					-1[2];
					(-1)[2];
				}
			}
			""",
			new Program()
				.withDecl(ClassDeclaration.dummy("Foo")
					.withMethod(Method.dummy("bar")
						.withBody(Statement.newBlock()
							.thenExpr(Expression.newInt(-1))
							.thenExpr(Expression.newInt(1)
								.withUnary(UnaryOperator.NEGATION))
							.thenExpr(Expression.newInt(1)
								.withCall("foo"))
							.thenExpr(Expression.newInt(1)
								.withCall("foo")
								.withUnary(UnaryOperator.NEGATION))
							.thenExpr(Expression.newInt(-1)
								.withCall("foo"))
							.thenExpr(Expression.newInt(1)
								.withArrayAccess(Expression.newInt(2)))
							.thenExpr(Expression.newInt(1)
								.withArrayAccess(Expression.newInt(2))
								.withUnary(UnaryOperator.NEGATION))
							.thenExpr(Expression.newInt(-1)
								.withArrayAccess(Expression.newInt(2))))))))
		);
		// @formatter:on
	}

	// This wrapper allows us to give the parameterized test cases nice labels
	public static record ParserTestCase(
		String label,
		String source,
		Program expectedProgram
	) {
		@Override
		public String toString() {
			return label();
		}
	}
}
