package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record BinaryOperatorExpression(
	Expression lhs,
	Expression rhs,
	BinaryOperator operator,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		p.add(lhs).add(" ").add(operator.getName()).add(" ").add(rhs);

		if (parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}
}
