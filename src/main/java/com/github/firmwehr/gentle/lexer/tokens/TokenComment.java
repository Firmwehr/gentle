package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public final class TokenComment extends Token {
	
	public enum CommentType {
		STAR, SINGLE_LINE
	}
	
	public final CommentType type;
	public final String text;
	
	private TokenComment(SourcePosition position, String text, CommentType type) {
		super(TokenType.COMMENT, position);
		this.type = type;
		this.text = text;
	}
	
	@Override
	public String toString() {
		return "TokenComment{" +
				"type=" + type +
				", text='" + text + '\'' +
				'}';
	}
	
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
				return new TokenComment(pos, s, CommentType.SINGLE_LINE);
			}
			default -> throw new LexerException("could not detect comment start with either '//' or '/*'", reader);
		}
	}
}
