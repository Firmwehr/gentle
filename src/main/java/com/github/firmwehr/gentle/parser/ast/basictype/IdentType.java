package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record IdentType(Ident name) implements BasicType {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(name);
	}
}
