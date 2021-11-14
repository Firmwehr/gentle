package com.github.firmwehr.gentle.parser.ast.expression;


import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record UnaryOperatorExpression(
	UnaryOperator operator,
	Expression expression,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		p.add(operator.getName()).add(expression);

		if (parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}
}
