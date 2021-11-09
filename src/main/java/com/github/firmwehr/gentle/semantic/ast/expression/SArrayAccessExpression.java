package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SType;

import java.util.Optional;

public record SArrayAccessExpression(
	SExpression expression,
	SExpression index
) implements SExpression {
	@Override
	public Optional<SType> approximateType() {
		return expression.approximateType().flatMap(SType::withDecrementedLevel);
	}
}
