package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record TokenComment(
	SourceSpan sourceSpan,
	String text
) implements Token {

	public static TokenComment create(LexReader reader) throws LexerException {
		var startPos = reader.position(); // important to capture position before we consume entire comment

		reader.expect("/*");
		var s = reader.readUntil("*/", true);
		s = s.substring(0, s.length() - 2); // strip trailing '*/'
		return new TokenComment(new SourceSpan(startPos, reader.endPositionOfRead()), s);
	}

	@Override
	public String format() {
		return "/*" + text + "*/";
	}

	@Override
	public TokenType tokenType() {
		return TokenType.COMMENT;
	}
}
