package com.github.firmwehr.gentle.parser.ast.type;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record BooleanType() implements Type {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("boolean");
	}
}
