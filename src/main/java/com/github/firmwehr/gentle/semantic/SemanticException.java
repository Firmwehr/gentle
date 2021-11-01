package com.github.firmwehr.gentle.semantic;

// FIXME: Proper SourceSpan and position and error message and dragons
public class SemanticException extends Exception {
	public SemanticException(String message) {
		super(message);
	}
}
