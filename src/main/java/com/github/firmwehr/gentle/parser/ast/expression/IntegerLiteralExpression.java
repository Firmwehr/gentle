package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record IntegerLiteralExpression(int value) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(Integer.toString(value));
	}
}
