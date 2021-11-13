package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record ArrayAccessExpression(
	Expression expression,
	Expression index,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		if (!omitParentheses) {
			p.add("(");
		}

		p.add(expression).add("[").add(index, true).add("]");

		if (!omitParentheses) {
			p.add(")");
		}
	}
}
