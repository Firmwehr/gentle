package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.tokens.IdentToken;

public record Ident(String ident) {
	public static Ident fromToken(IdentToken token) {
		return new Ident(token.ident());
	}
}
