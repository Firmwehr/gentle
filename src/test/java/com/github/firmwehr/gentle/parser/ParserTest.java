package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourcePosition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.github.firmwehr.gentle.testutil.Equality.equalExceptSourcePosition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTest {
	private static Parser fromText(String text) throws LexerException {
		Source source = new Source(text);
		return Parser.fromLexer(source, new Lexer(source, true));
	}

	@ParameterizedTest
	@ArgumentsSource(ParserTestCaseProvider.class)
	public void parse_shouldConstructAstForSyntacticallyCorrectPrograms(ParserTestCase testCase)
		throws LexerException, ParseException {

		Parser parser = fromText(testCase.source());
		Program actualProgram = parser.parse();

		System.out.println(PrettyPrinter.format(actualProgram));
		System.out.println();
		System.out.println(PrettyPrinter.format(testCase.expectedProgram()));

		assertThat(actualProgram).is(equalExceptSourcePosition(testCase.expectedProgram()));
	}

	private record FailingParserTestCase(
		String label,
		String source,
		// pde = parser defined error
		SourcePosition expectedPde
	) {
		// expects source to have unix line endings
		public static FailingParserTestCase fromPos(String label, String source, int line, int column) {
			int offset = source.lines().limit(line - 1).mapToInt(line_ -> line_.length() + 1).sum() + column - 1;
			return new FailingParserTestCase(label, source, new SourcePosition(offset, line, column));
		}

		@Override
		public String toString() {
			return label;
		}
	}

	@ParameterizedTest
	@MethodSource("provideSyntacticallyIncorrectPrograms")
	public void parse_shouldFailForSyntacticallyIncorrectPrograms(FailingParserTestCase testCase)
		throws LexerException {

		Parser parser = fromText(testCase.source());
		ParseException parseException = assertThrows(ParseException.class, parser::parse);

		System.out.println(parseException.getMessage());

		assertThat(parseException.getToken().sourceSpan().startOffset()).isEqualTo(testCase.expectedPde().offset());
	}

	private static List<Arguments> provideSyntacticallyIncorrectPrograms() {
		// @formatter:off
		return Stream.of(
			// ClassDeclaration
			FailingParserTestCase.fromPos("missing class keyword",
				"""
                {}
                """,
				1, 1
			),
			FailingParserTestCase.fromPos("missing class name",
				"""
                class {}
                """,
				1, 7
			),
			FailingParserTestCase.fromPos("missing opening brace",
				"""
				class C = 42;
				""",
				1, 9
			),
			FailingParserTestCase.fromPos("missing closing brace",
				"""
                class C {
                """,
				2, 1
			),
			// ClassMember
			FailingParserTestCase.fromPos("missing public modifier",
				"""
                class C { int foo() {} }
                """,
				1, 11
			),
			FailingParserTestCase.fromPos("main method returning int",
				"""
				class C { public static int main(String[] args) {} }
				""",
				1, 25
			),
			FailingParserTestCase.fromPos("public final field",
				"""
                class C { public final int i = 42; }
                """,
				1, 18
			),
			FailingParserTestCase.fromPos("multiple field declarations",
				"""
                class C { public int i, j = 42; }
                """,
				1, 23
			),
			FailingParserTestCase.fromPos("missing main parameters",
				"""
                class C { public static void main; }
                """,
				1, 34
			),
			FailingParserTestCase.fromPos("missing method block",
				"""
                class C { public int foo() public int bar(); }
                """,
				1, 28
			),
			FailingParserTestCase.fromPos("incomplete throws clause",
				"""
				class C { public int foo() throws; }
				""",
				1, 34
			),
			FailingParserTestCase.fromPos("trailing parameter comma",
				"""
                class C { public int foo(int x,); }
                """,
				1, 32
			),
			FailingParserTestCase.fromPos("invalid parameter",
				"""
                class C { public int foo(int *x); }
                """,
				1, 30
			),
			// Type
			FailingParserTestCase.fromPos("index in array type",
				"""
				class C { public int[42] a; }
				""",
				1, 22
			),
			FailingParserTestCase.fromPos("float type",
				"""
                class C { public float bar(); }
                """,
				1, 18
			),
			// Block
			FailingParserTestCase.fromPos("empty statement where a block should be",
				"""
				class C { public void foo(); }
				""",
				1, 28
			),
			FailingParserTestCase.fromPos("mismatched block braces",
				"""
				class C { public int foo() {{{}{}} public int bar() {} }
				""",
				1, 36
			),
			// Statement
			FailingParserTestCase.fromPos("lone else",
				"""
				class C { public int f() { else; }
				""",
				1, 28
			),
			FailingParserTestCase.fromPos("missing if parens",
				"""
				class C { public int f() { if x == 42; }
				""",
				1, 31
			),
			FailingParserTestCase.fromPos("missing else statement",
				"""
                class C { public int f() { if (x == 42); else }
                """,
				1, 47
			),
			FailingParserTestCase.fromPos("missing while parens",
				"""
				class C { public int f() { while b; }
				""",
				1, 34
			),
			FailingParserTestCase.fromPos("invalid return expression",
				"""
				class C { public int f() { return ++p; }
				""",
				1, 35
			),
			FailingParserTestCase.fromPos("incomplete local variable declaration",
				"""
                class C {
                    public int f() { int = 42; }
                }
                """,
				2, 26
			),
			FailingParserTestCase.fromPos("float as identifier",
				"""
                class C {
                    public int f() { int float = 42; }
                }
                """,
				2, 26
			),
			FailingParserTestCase.fromPos("incomplete local variable declaration 2",
				"""
				class C {
					public int f() { int x = }
				}
				""",
				2, 27
			),
			// Special cases
			FailingParserTestCase.fromPos("leading zero integer literal",
				"""
                class C {
                    public int eight() { return 010; }
                }
                """,
				2, 34
            ),
			FailingParserTestCase.fromPos("confusing error position",
				"""
                class C {
                    public int foo() {   {{ }   }
                    void bar() {}
                }
                """,
				3, 13
			)
		).map(Arguments::of).toList();
		// @formatter:on
	}
}
