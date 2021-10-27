package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record BooleanLiteralExpression(boolean value) implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		if (value) {
			p.add("true");
		} else {
			p.add("false");
		}
	}
}
