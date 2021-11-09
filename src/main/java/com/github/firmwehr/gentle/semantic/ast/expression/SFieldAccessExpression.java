package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.SType;

import java.util.Optional;

public record SFieldAccessExpression(
	SExpression expression,
	SField field
) implements SExpression {
	@Override
	public Optional<SType> approximateType() {
		return Optional.of(field.type());
	}
}
