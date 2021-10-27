package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record MethodInvocationOp(
	Ident name,
	List<Expression> arguments
) implements PostfixOp {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(".").add(name).add("(").indent().addAll(arguments).unindent().add(")");
	}
}
