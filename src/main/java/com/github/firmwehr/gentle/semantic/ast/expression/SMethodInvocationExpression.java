package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.List;
import java.util.Optional;

public record SMethodInvocationExpression(
	SExpression expression,
	SMethod method,
	List<SExpression> arguments
) implements SExpression {
	@Override
	public Optional<SNormalType> approximateType() {
		return method.returnType();
	}
}
