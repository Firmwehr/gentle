package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.Method;
import com.github.firmwehr.gentle.semantic.ast.Type;

import java.util.List;
import java.util.Optional;

public record MethodInvocationExpression(
	Expression expression,
	Method method,
	List<Expression> arguments
) implements Expression {
	@Override
	public Optional<Type> approximateType() {
		return method.returnType();
	}
}
