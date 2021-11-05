package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.parser.tokens.IdentToken;

public record Ident(String ident) implements PrettyPrint {
	public static Ident fromToken(IdentToken token) {
		return new Ident(token.ident());
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(ident);
	}
}
