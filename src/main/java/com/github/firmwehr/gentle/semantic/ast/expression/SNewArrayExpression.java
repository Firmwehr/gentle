package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

public record SNewArrayExpression(
	SNormalType type,
	SExpression size
) implements SExpression {
}
