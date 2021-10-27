package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record TokenComment(
	SourceSpan sourceSpan,
	String text,
	CommentType type
) implements Token {

	public static TokenComment create(LexReader reader) throws LexerException {
		var peek = reader.peek(2);
		var startPos = reader.position(); // important to capture position before we consume entire comment
		switch (peek) {
			case "//" -> {
				reader.expect("//");
				return new TokenComment(new SourceSpan(startPos, reader.endPositionOfRead()), reader.readLine(),
					CommentType.SINGLE_LINE);
			}
			case "/*" -> {
				reader.expect("/*");
				var s = reader.readUntil("*/", true);
				s = s.substring(0, s.length() - 2); // strip trailing '*/'
				return new TokenComment(new SourceSpan(startPos, reader.endPositionOfRead()), s, CommentType.STAR);
			}
			default -> throw new LexerException("could not detect comment start with either '//' or '/*'", reader);
		}
	}

	@Override
	public String format() {
		return switch (type) {
			case STAR -> "/*" + text + "*/";
			case SINGLE_LINE -> "//" + text;
		};
	}

	@Override
	public TokenType tokenType() {
		return TokenType.COMMENT;
	}

	public enum CommentType {
		STAR,
		SINGLE_LINE
	}
}
