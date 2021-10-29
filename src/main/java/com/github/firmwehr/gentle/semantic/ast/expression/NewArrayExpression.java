package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.Type;

import java.util.Optional;

public record NewArrayExpression(
	Type type,
	Expression size
) implements Expression {
	@Override
	public Optional<Type> approximateType() {
		return Optional.of(type);
	}
}
