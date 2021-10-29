package com.github.firmwehr.gentle.parser.ast.basictype;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record IntType() implements BasicType {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("int");
	}
}
