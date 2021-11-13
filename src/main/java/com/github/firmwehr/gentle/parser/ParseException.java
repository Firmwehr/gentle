package com.github.firmwehr.gentle.parser;

import com.github.firmwehr.gentle.parser.tokens.Token;
import com.github.firmwehr.gentle.source.Source;

import static org.fusesource.jansi.Ansi.ansi;

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
		return ansi().bold() + "Unexpected " + ansi().boldOff() + ansi().fgRed() + token.format() + ansi().reset() +
			"\n" + source.formatMessageAt(token.sourceSpan(), description);
	}
}
