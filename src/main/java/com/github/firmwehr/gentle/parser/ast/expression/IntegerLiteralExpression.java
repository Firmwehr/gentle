package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.math.BigInteger;

public record IntegerLiteralExpression(
	BigInteger value,
	SourceSpan sourceSpan
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add(value.toString());
	}
}
