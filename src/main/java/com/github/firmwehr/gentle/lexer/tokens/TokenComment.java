package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public final class TokenComment extends GentleToken {
	
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
		CommentFetcher fetcher;
		CommentType type;
		try {
			reader.expect("//");
			type = CommentType.SINGLE_LINE;
			fetcher = LexReader::readLine;
		} catch (LexerException ignore) {
			try {
				reader.expect("/*");
				type = CommentType.STAR;
				fetcher = r -> {
					// strip trailing '*/'
					var s = r.readUntil("*/", true);
					return s.substring(0, s.length() - 2);
				};
			} catch (LexerException ignore2) {
				throw new LexerException("could not detect command start with either '//' or '/*'", reader);
			}
		}
		return new TokenComment(reader.position(), fetcher.fetch(reader), type);
	}
	
	@FunctionalInterface
	private interface CommentFetcher {
		
		String fetch(LexReader reader) throws LexerException;
	}
	
}
