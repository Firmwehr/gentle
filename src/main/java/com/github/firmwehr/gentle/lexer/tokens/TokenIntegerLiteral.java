package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.math.BigInteger;

public record TokenIntegerLiteral(
	SourceSpan sourceSpan,
	BigInteger number
) implements Token {

	public static TokenIntegerLiteral create(LexReader reader) throws LexerException {
		var startPos = reader.position();
		try {
			var str = reader.readUntilOrEndOfFile(v -> !Character.isDigit(v), false);
			if (str.startsWith("0") && str.length() > 1) {
				throw new LexerException("leading zero is not allowed", reader);
			}

			var bigint = new BigInteger(str);
			return new TokenIntegerLiteral(new SourceSpan(startPos, reader.endPositionOfRead()), bigint);
		} catch (NumberFormatException e) {
			throw new LexerException("not a number", reader, e);
		}
	}

	@Override
	public String format() {
		return "integer literal " + number.toString();
	}

	@Override
	public TokenType tokenType() {
		return TokenType.INTEGER_LITERAL;
	}
}
