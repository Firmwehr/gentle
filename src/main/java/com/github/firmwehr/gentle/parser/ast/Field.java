package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record Field(
	Type type,
	Ident name
) implements PrettyPrint {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("Field{").indent().newline();
		p.add("type = ").add(type).newline();
		p.add("name = ").add(name).newline();
		p.unindent().add("}");
	}
}
