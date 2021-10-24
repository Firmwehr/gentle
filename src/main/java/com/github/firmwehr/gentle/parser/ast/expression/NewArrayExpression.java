package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.type.Type;

public record NewArrayExpression(
	Type type,
	Expression index
) implements Expression {
}
