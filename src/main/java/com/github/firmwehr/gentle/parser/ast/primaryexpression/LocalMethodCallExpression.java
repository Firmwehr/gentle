package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record LocalMethodCallExpression(
	Ident name,
	List<Expression> arguments
) implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(name).add("(").indent().addAll(arguments).unindent().add(")");
	}
}
