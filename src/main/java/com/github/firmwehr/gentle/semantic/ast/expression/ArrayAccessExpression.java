package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.Type;

import java.util.Optional;

public record ArrayAccessExpression(
	Expression expression,
	Expression index
) implements Expression {
	@Override
	public Optional<Type> approximateType() {
		return expression.approximateType().flatMap(Type::withDecrementedLevel);
	}
}
