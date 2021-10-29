package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;

public record UnaryOperatorExpression(
	UnaryOperator operator,
	Expression expression
) implements Expression {
}
