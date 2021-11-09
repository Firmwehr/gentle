package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;

public record SBinaryOperatorExpression(
	SExpression lhs,
	SExpression rhs,
	BinaryOperator operator
) implements SExpression {
}
