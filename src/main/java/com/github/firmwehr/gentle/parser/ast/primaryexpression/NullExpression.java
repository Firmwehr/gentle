package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record NullExpression() implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("null");
	}
}
