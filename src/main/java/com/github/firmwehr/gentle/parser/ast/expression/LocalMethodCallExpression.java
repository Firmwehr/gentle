package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.List;

public record LocalMethodCallExpression(
	Ident name,
	List<Expression> arguments,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		if (!omitParentheses) {
			p.add("(");
		}

		p.add(name).add("(").addAll(arguments, ", ", false, true).add(")");

		if (!omitParentheses) {
			p.add(")");
		}
	}
}
