package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SType;

import java.util.Optional;

public record SNewArrayExpression(
	SType type,
	SExpression size
) implements SExpression {
	@Override
	public Optional<SType> approximateType() {
		return Optional.of(type);
	}
}
