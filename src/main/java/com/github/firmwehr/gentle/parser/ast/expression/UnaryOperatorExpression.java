package com.github.firmwehr.gentle.parser.ast.expression;


public record UnaryOperatorExpression(
	UnaryOperator operator,
	Expression expression
) implements Expression {
}
