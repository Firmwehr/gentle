package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.Optional;

public record SNewArrayExpression(
	SNormalType type,
	SExpression size
) implements SExpression {
	@Override
	public Optional<SNormalType> approximateType() {
		return Optional.of(type);
	}
}
