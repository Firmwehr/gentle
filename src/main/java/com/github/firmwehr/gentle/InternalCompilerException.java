package com.github.firmwehr.gentle;

public class InternalCompilerException extends RuntimeException {

	private final String message;

	public InternalCompilerException(String message) {
		this.message = message;

	}

	public InternalCompilerException(String message, Exception cause) {
		super(cause);
		this.message = message;
	}

	@Override
	public String getMessage() {
		return "Gentle panic: " + message;
	}
}
