package com.github.firmwehr.gentle.parser.prettyprint;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ParseException;
import com.github.firmwehr.gentle.parser.Parser;
import com.github.firmwehr.gentle.parser.ParserTestCaseProvider;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.source.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;

public class PrettyPrinterTest {
	private static Program parse(String text) throws LexerException, ParseException {
		Source source = new Source(text);
		return Parser.fromLexer(source, new Lexer(source, true)).parse();
	}

	/*
	 * Basic test for the given example from https://pp.ipd.kit.edu/lehre/WS202122/compprakt/.
	 * Also on sheet 4.
	 */
	@Test
	public void format_shouldCorrectlyPrettyPrintTheGivenExample() throws ParseException, LexerException {
		var exampleInput = """
			class HelloWorld
			{
				public int c;
				public boolean[] array;
				public static /* blabla */ void main(String[] args)
				{ System.out.println( (43110 + 0) );
					boolean b = true && (!false);
					if (23+19 == (42+0)*1)
						b = (0 < 1);
					else if (!array[2+2]) {
						int x = 0;;
						x = x+1;
					} else {
						new HelloWorld().bar(42+0*1, -1);
					}
				}
				public int bar(int a, int b) { return c = (a+b); }
			}
			""";
		var exampleOutput = """
			class HelloWorld {
				public int bar(int a, int b) {
					return c = (a + b);
				}
				public static void main(String[] args) {
					(System.out).println(43110 + 0);
					boolean b = true && (!false);
					if ((23 + 19) == ((42 + 0) * 1))
						b = (0 < 1);
					else if (!(array[2 + 2])) {
						int x = 0;
						x = (x + 1);
					} else {
						(new HelloWorld()).bar(42 + (0 * 1), -1);
					}
				}
				public boolean[] array;
				public int c;
			}"""; // Newline is not inserted by PrettyPrinter but by println

		var program = parse(exampleInput);
		assertThat(PrettyPrinter.format(program)).isEqualTo(exampleOutput);
	}

	@ParameterizedTest
	@ArgumentsSource(ParserTestCaseProvider.class)
	public void format_shouldReturnParsableStrings(ParserTestCaseProvider.ParserTestCase testCase) throws ParseException, LexerException {
		Program program1 = parse(testCase.source());
		String prettyPrinted = PrettyPrinter.format(program1);
		Program program2 = parse(prettyPrinted);

		// Not comparing program1 and program2 directly as format may reorder class members
		assertThat(prettyPrinted).isEqualTo(PrettyPrinter.format(program2));
	}
}
