package com.github.firmwehr.gentle.lexer;

import com.github.firmwehr.gentle.source.Source;
import com.github.firmwehr.gentle.source.SourceSpan;

import static org.fusesource.jansi.Ansi.ansi;

public class LexerException extends Exception {

	private final Source source;
	private final int offset;
	private final String description;

	public LexerException(String description, StringReader reader) {
		this.source = reader.getSource();
		this.offset = reader.getPosition();
		this.description = description;
	}

	@Override
	public String getMessage() {
		return ansi().bold() + "Failed to lex token\n" + ansi().boldOff() +
			source.formatMessageAt(new SourceSpan(offset, offset + 1), description);
	}
}
