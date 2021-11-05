package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record BooleanLiteralExpression(boolean value) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		if (value) {
			p.add("true");
		} else {
			p.add("false");
		}
	}
}
