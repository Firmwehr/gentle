package com.github.firmwehr.gentle;

/**
 * To be used in cases where a programmer mistake might lead to errors. For example this could be thrown when a branch
 * can't be taken because of some violated data structure invariant.
 */
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
