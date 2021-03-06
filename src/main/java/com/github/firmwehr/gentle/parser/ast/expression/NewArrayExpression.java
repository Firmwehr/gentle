package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;
import com.google.common.base.Preconditions;

public record NewArrayExpression(
	Type type,
	Expression size,
	SourceSpan sourceSpan
) implements Expression {
	public NewArrayExpression {
		Preconditions.checkArgument(type.arrayLevel() >= 1);
	}

	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		p.add("new ").add(type.basicType()).add("[").add(size, Parentheses.OMIT).add("]");
		for (int i = 0; i < type.arrayLevel() - 1; i++) {
			p.add("[]");
		}

		if (parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}
}
