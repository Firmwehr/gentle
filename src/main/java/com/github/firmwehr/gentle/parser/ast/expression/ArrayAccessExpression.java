package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record ArrayAccessExpression(
	Expression expression,
	Expression index
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(expression).add("[").add(index).add("]");
	}
}
