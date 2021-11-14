package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record Field(
	Type type,
	Ident name
) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		p.add("public ").add(type).add(" ").add(name).add(";");
	}
}
