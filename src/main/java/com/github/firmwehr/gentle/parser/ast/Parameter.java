package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record Parameter(
	Type type,
	Ident name
) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Parameter(").add(type).add(" ").add(name).add(")");
	}
}
