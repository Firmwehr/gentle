package com.github.firmwehr.gentle.parser.ast.type;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record IntType() implements Type {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("int");
	}
}
