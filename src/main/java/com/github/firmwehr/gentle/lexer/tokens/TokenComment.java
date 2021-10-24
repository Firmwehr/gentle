package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourcePosition;

public record TokenComment(
	SourcePosition position,
	String text,
	CommentType type
) implements Token {

	public static TokenComment create(LexReader reader) throws LexerException {
		var peek = reader.peek(2);
		var pos = reader.position(); // important to capture position before we consume entire comment
		switch (peek) {
			case "//" -> {
				reader.expect("//");
				return new TokenComment(pos, reader.readLine(), CommentType.SINGLE_LINE);
			}
			case "/*" -> {
				reader.expect("/*");
				var s = reader.readUntil("*/", true);
				s = s.substring(0, s.length() - 2); // strip trailing '*/'
				return new TokenComment(pos, s, CommentType.STAR);
			}
			default -> throw new LexerException("could not detect comment start with either '//' or '/*'", reader);
		}
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
