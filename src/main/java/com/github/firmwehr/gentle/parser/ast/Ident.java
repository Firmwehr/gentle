package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;
import com.github.firmwehr.gentle.source.SourceSpan;

public record Ident(
	String ident,
	SourceSpan sourceSpan
) implements PrettyPrint {
	public static Ident fromToken(IdentToken token) {
		return new Ident(token.ident(), token.sourceSpan());
	}

	/**
	 * A dummy identifier with bogus SourcePosition, meant only to be used in tests.
	 */
	public static Ident dummy(String name) {
		return new Ident(name, SourceSpan.dummy());
	}

	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		p.add(ident);
	}
}
