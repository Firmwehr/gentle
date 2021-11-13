package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.lexer.Lexer;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.parser.ast.Program;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.Source;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

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
	public void parse_shouldConstructAstForSyntacticallyCorrectPrograms(ParserTestCaseProvider.ParserTestCase testCase)
		throws LexerException, ParseException {

		Parser parser = fromText(testCase.source());
		Program actualProgram = parser.parse();

		System.out.println(PrettyPrinter.format(actualProgram));
		System.out.println();
		System.out.println(PrettyPrinter.format(testCase.expectedProgram()));

		assertThat(actualProgram).is(equalExceptSourcePosition(testCase.expectedProgram()));
	}

	@ParameterizedTest
	@ArgumentsSource(FailingParserTestCaseProvider.class)
	public void parse_shouldFailForSyntacticallyIncorrectPrograms(FailingParserTestCaseProvider.FailingParserTestCase testCase)
		throws LexerException {

		Parser parser = fromText(testCase.source());
		ParseException parseException = assertThrows(ParseException.class, parser::parse);

		System.out.println(parseException.getMessage());

		assertThat(parseException.getToken().sourceSpan().startOffset()).isEqualTo(testCase.expectedPdeOffset());
	}

}
