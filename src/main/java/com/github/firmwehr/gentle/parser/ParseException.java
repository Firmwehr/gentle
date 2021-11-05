package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.source.Source;

public class ParseException extends Exception {
	private final Source source;
	private final Token token;
	private final String description;

	public ParseException(Source source, Token token, String description) {
		this.source = source;
		this.token = token;
		this.description = description;
	}

	@Override
	public String getMessage() {
		return source.formatErrorAtOffset(token.sourceSpan().startOffset(), "Unexpected " + token.format(), description);
	}
}
