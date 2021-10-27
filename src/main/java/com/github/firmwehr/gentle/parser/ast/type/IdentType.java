package com.github.firmwehr.gentle.parser.ast.type;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record IdentType(Ident name) implements Type {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(name);
	}
}
