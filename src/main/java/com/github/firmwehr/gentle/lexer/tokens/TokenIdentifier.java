package com.github.firmwehr.gentle.lexer.tokens;

import com.github.firmwehr.gentle.GentleCompiler;
import com.github.firmwehr.gentle.lexer.LexReader;
import com.github.firmwehr.gentle.lexer.LexerException;
import com.github.firmwehr.gentle.lexer.TokenType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public record TokenIdentifier(
	SourceSpan sourceSpan,
	String id
) implements Token {

	private static final Predicate<String> ASCII_IDENT_PREDICATE =
		Pattern.compile("[a-zA-Z_]+([a-zA-Z_0-9]*)").asMatchPredicate();

	public static TokenIdentifier create(LexReader reader) throws LexerException {
		if (!Character.isJavaIdentifierStart(reader.peek())) {
			throw new LexerException("does not start with identifier codepoint", reader);
		}
		var startPos = reader.position();
		var id = reader.readUntilOrEndOfFile(cp -> !Character.isJavaIdentifierPart(cp), false);
		if (!GentleCompiler.unicodeEnabled() && !ASCII_IDENT_PREDICATE.test(id)) {
			throw new LexerException("identifier does not follow requirements, please enable unicode support", reader);
		}

		return new TokenIdentifier(new SourceSpan(startPos, reader.endPositionOfRead()), id);
	}

	@Override
	public String format() {
		return "identifier " + id;
	}

	@Override
	public TokenType tokenType() {
		return TokenType.IDENTIFIER;
	}
}
