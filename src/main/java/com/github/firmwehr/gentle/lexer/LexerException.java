package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.source.Source;

public class LexerException extends Exception {

	private final Source source;
	private final int offset;

	public LexerException(String message, StringReader reader) {
		super(message);
		this.source = reader.getSource();
		this.offset = reader.getPosition();
	}

	@Override
	public String getMessage() {
		return source.formatErrorAtPosition(offset, "Failed to lex token", super.getMessage());
	}
}
