package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

public record SUnaryOperatorExpression(
	UnaryOperator operator,
	SExpression expression
) implements SExpression {
	@Override
	public SExprType type() {
		return expression.type();
	}
}
