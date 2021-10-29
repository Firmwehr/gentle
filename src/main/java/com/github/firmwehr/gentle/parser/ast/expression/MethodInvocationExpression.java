package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record MethodInvocationExpression(
	Expression expression,
	Ident name,
	List<Expression> arguments
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(expression).add(".").add(name).add("(").indent().addAll(arguments).unindent().add(")");
	}
}
