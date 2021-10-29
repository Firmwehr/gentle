package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.Field;
import com.github.firmwehr.gentle.semantic.ast.Type;

import java.util.Optional;

public record FieldAccessExpression(
	Expression expression,
	Field field
) implements Expression {
	@Override
	public Optional<Type> approximateType() {
		return Optional.of(field.type());
	}
}
