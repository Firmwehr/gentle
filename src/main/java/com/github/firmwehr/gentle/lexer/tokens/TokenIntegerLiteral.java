package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.SourcePosition;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;

public final class TokenIntegerLiteral extends Token {
	
	public final int number;
	
	private TokenIntegerLiteral(SourcePosition position, int number) {
		super(TokenType.INTEGER_LITERAL, position);
		this.number = number;
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
			if (str.startsWith("0") && str.length() > 1)
				throw new LexerException("leading zero is not allowed", reader);
			
			int number = Integer.parseInt(str);
			return new TokenIntegerLiteral(reader.position(), number);
		} catch (NumberFormatException e) {
			throw new LexerException("not a number", reader, e);
		}
	}
}
