package com.github.firmwehr.gentle.parser.ast.expression;


import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record UnaryOperatorExpression(
	UnaryOperator operator,
	Expression expression
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("(").add(operator.getName()).add(expression).add(")");
	}
}
