package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

import java.util.List;

public record SMethodInvocationExpression(
	SExpression expression,
	SMethod method,
	List<SExpression> arguments
) implements SExpression {
	@Override
	public SExprType type() {
		return method.returnType().asExprType();
	}
}
