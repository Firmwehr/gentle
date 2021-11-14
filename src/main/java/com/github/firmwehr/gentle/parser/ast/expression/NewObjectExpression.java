package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

public record NewObjectExpression(
	Ident name,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		p.add("new ").add(name).add("()");

		if (parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}
}
