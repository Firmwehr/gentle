package com.github.firmwehr.gentle.parser.ast.expression;

public record BinaryOperatorExpression(
	Expression lhs,
	Expression rhs,
	BinaryOperator operator
) implements Expression {
}
