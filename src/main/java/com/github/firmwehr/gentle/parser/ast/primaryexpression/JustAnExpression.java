package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record JustAnExpression(Expression expression) implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(expression);
	}
}
