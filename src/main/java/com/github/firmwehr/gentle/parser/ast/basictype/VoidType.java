package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record VoidType() implements BasicType {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("void");
	}
}
