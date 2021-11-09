package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.Optional;

public record SArrayAccessExpression(
	SExpression expression,
	SExpression index
) implements SExpression {
	@Override
	public Optional<SNormalType> approximateType() {
		return expression.approximateType().flatMap(SNormalType::withDecrementedLevel);
	}
}
