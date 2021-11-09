package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

public record SArrayAccessExpression(
	SExpression expression,
	SExpression index,
	SNormalType type
) implements SExpression {
}
