package com.github.firmwehr.gentle.lexer;

public class LexerException extends Exception {

	private final LexReader reader;

	public LexerException(String message, LexReader reader, Throwable throwable) {
		super(message, throwable);
		this.reader = reader.fork();
	}

	public LexerException(String message, LexReader reader) {
		super(message);
		this.reader = reader.fork();
	}

	@Override
	public String getMessage() {
		return reader.getSource().formatErrorAtPosition(reader.position(), "Failed to lex token", super.getMessage());
	}
}
