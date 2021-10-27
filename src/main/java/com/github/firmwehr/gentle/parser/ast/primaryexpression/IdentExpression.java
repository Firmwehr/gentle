package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record IdentExpression(Ident name) implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(name);
	}
}
