package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.type.ArrayType;
import com.github.firmwehr.gentle.parser.ast.type.Type;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record NewArrayExpression(
	Type type,
	Expression size
) implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		if (type instanceof ArrayType at) {
			// TODO Format this closer to actual syntax?
			p.add("new ").add(at.subtype()).add("[").add(size).add("]");
		} else {
			throw new IllegalStateException("there must be at least one ArrayType");
		}
	}
}
