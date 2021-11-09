package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;

public record SUnaryOperatorExpression(
	UnaryOperator operator,
	SExpression expression
) implements SExpression {
}
