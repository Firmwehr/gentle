package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record ThisExpression() implements PrimaryExpression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("this");
	}
}
