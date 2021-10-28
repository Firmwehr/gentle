package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.google.common.base.Preconditions;

public record NewArrayExpression(
	Type type,
	Expression size
) implements PrimaryExpression {
	public NewArrayExpression {
		Preconditions.checkArgument(type.arrayLevel() >= 1);
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("new 4").add(type.basicType()).add("[").add(size).add("]");
		for (int i = 0; i < type.arrayLevel() - 1; i++) {
			p.add("[]");
		}
	}
}
