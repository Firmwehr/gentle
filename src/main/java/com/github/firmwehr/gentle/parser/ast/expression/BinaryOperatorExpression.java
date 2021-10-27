package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record BinaryOperatorExpression(
	Expression lhs,
	Expression rhs,
	BinaryOperator operator
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("(").add(lhs).add(" ").add(operator.getName()).add(" ").add(rhs).add(")");
	}
}
