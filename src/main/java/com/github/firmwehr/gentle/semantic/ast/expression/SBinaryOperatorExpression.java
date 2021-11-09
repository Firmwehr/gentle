package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.BinaryOperator;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

public record SBinaryOperatorExpression(
	SExpression lhs,
	SExpression rhs,
	BinaryOperator operator,
	SExprType type
) implements SExpression {
}
