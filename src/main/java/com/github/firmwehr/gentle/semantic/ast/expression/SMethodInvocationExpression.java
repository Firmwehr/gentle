package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.SType;

import java.util.List;
import java.util.Optional;

public record SMethodInvocationExpression(
	SExpression expression,
	SMethod method,
	List<SExpression> arguments
) implements SExpression {
	@Override
	public Optional<SType> approximateType() {
		return method.returnType();
	}
}
