package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

public record SFieldAccessExpression(
	SExpression expression,
	SField field
) implements SExpression {
	@Override
	public SExprType type() {
		return field.type();
	}
}
