package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;

public record BinaryOperatorExpression(
	Expression lhs,
	Expression rhs,
	BinaryOperator operator
) implements Expression {
}
