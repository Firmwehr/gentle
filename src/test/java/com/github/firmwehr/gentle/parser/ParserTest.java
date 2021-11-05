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
			new FailingParserTestCase("missing class keyword",
				"""
                {}
                """,
				new SourcePosition(0, 1, 1)
			),
			new FailingParserTestCase("missing class name",
				"""
                class {}
                """,
				new SourcePosition(6, 1, 7)
			),
			new FailingParserTestCase("missing opening brace",
				"""
				class C = 42;
				""",
				new SourcePosition(8, 1, 9)
			),
			new FailingParserTestCase("missing closing brace",
				"""
                class C {
                """,
				new SourcePosition(10, 2, 1)
			),
			// ClassMember
			new FailingParserTestCase("missing public modifier",
				"""
                class C { int foo() {} }
                """,
				new SourcePosition(10, 1, 11)
			),
			new FailingParserTestCase("main method returning int",
				"""
				class C { public static int main(String[] args) {} }
				""",
				new SourcePosition(24, 1, 25)
			),
			new FailingParserTestCase("public final field",
				"""
                class C { public final int i = 42; }
                """,
				new SourcePosition(17, 1, 18)
			),
			new FailingParserTestCase("multiple field declarations",
				"""
                class C { public int i, j = 42; }
                """,
				new SourcePosition(22, 1, 23)
			),
			new FailingParserTestCase("missing main parameters",
				"""
                class C { public static void main; }
                """,
				new SourcePosition(33, 1, 34)
			),
			new FailingParserTestCase("missing method block",
				"""
                class C { public int foo() public int bar(); }
                """,
				new SourcePosition(27, 1, 28)
			),
			new FailingParserTestCase("incomplete throws clause",
				"""
				class C { public int foo() throws; }
				""",
				new SourcePosition(33, 1, 34)
			),
			new FailingParserTestCase("trailing parameter comma",
				"""
                class C { public int foo(int x,); }
                """,
				new SourcePosition(31, 1, 32)
			),
			new FailingParserTestCase("invalid parameter",
				"""
                class C { public int foo(int *x); }
                """,
				new SourcePosition(29, 1, 30)
			),
			// Type
			new FailingParserTestCase("index in array type",
				"""
				class C { public int[42] a; }
				""",
				new SourcePosition(21, 1, 22)
			),
			new FailingParserTestCase("float type",
				"""
                class C { public float bar(); }
                """,
				new SourcePosition(17, 1, 18)
			),
			// Block
			new FailingParserTestCase("empty statement where a block should be",
				"""
				class C { public void foo(); }
				""",
				new SourcePosition(27, 1, 28)
			),
			new FailingParserTestCase("mismatched block braces",
				"""
				class C { public int foo() {{{}{}} public int bar() {} }
				""",
				new SourcePosition(35, 1, 36)
			),
			// Statement
			// FIXME
			// Special cases
			new FailingParserTestCase("leading zero integer literal",
				"""
                class C {
                    public int eight() { return 010; }
                }
                """,
				new SourcePosition(43, 2, 34)
            ),
			new FailingParserTestCase("confusing error position",
				"""
                class C {
                    public int foo() {   {{ }   }
                    void bar() {}
                }
                """,
				new SourcePosition(56, 3, 13)
			)
		).map(Arguments::of).toList();
		// @formatter:on
	}
}
