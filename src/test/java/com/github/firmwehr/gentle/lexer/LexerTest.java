package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.lexer.tokens.GentleToken;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class LexerTest {

	private static final String TEST_INPUT = """
			void   void
			void
			""";
	
	private static final String TEST_INPUT2 = """
			class int []()(int void)void;
			""";
	
	@Test
	public void basicLexerTest() throws Exception {
		GentleLexer lexer = new GentleLexer(TEST_INPUT2);
		var list = readUntilEOF(lexer);
	}
	
	private static List<GentleToken> readUntilEOF(GentleLexer lexer) throws LexerException {
		var list = new ArrayList<GentleToken>();
		
		TokenType tokenType;
		do {
			var token = lexer.nextToken();
			list.add(token);
			tokenType = token.tokenType();
		} while (tokenType != TokenType.EOF);
		
		return list;
	}
}
