package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public final class TokenIntegerLiteral extends GentleToken {
	
	public int number;
	
	private TokenIntegerLiteral(SourcePosition position, int number) {
		super(TokenType.INTEGER_LITERAL, position);
	}
	
	@Override
	public String toString() {
		return "TokenIntegerLiteral{" +
				"number=" + number +
				'}';
	}
	
	public static TokenIntegerLiteral create(LexReader reader) throws LexerException {
		try {
			var str = reader.readUntilOrEndOfFile(v -> !Character.isDigit(v), false);
			int number = Integer.parseInt(str);
			return new TokenIntegerLiteral(reader.position(), number);
		} catch (NumberFormatException e) {
			throw new LexerException("not a number", reader, e);
		}
	}
}
