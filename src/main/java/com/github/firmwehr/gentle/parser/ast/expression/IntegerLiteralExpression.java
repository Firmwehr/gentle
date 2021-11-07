package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.math.BigInteger;

public record IntegerLiteralExpression(BigInteger value) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(value.toString());
	}
}
