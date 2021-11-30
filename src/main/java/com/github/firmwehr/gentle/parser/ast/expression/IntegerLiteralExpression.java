package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.math.BigInteger;

public record IntegerLiteralExpression(
	BigInteger absValue,
	boolean negated,
	SourceSpan sourceSpan
) implements Expression {
	public IntegerLiteralExpression {
		if (absValue.signum() < 0) {
			throw new IllegalArgumentException("absValue must not be negative");
		}
	}

	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		if (negated && parens == Parentheses.INCLUDE) {
			p.add("(");
		}

		if (negated) {
			p.add("-");
		}
		p.add(absValue.toString());

		if (negated && parens == Parentheses.INCLUDE) {
			p.add(")");
		}
	}

	public BigInteger value() {
		return negated ? absValue.negate() : absValue;
	}
}
