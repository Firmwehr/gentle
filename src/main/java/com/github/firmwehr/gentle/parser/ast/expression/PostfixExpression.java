package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record PostfixExpression(
	Expression expression,
	PostfixOp postfixOp
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(expression).add(postfixOp);
	}
}
