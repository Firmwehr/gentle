package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.List;

public record MethodInvocationExpression(
	Expression expression,
	Ident name,
	List<Expression> arguments,
	SourceSpan postfixSpan,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		p.add(expression).add(".").add(name).add("(").addAll(arguments, ", ", false, Parentheses.OMIT).add(")");

		if (parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}
}
