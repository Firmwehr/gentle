package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;

import java.util.List;
import java.util.Optional;

public record SMethodInvocationExpression(
	SExpression expression,
	SMethod method,
	List<SExpression> arguments
) implements SExpression {
	@Override
	public SExprType type() {
		Optional<SNormalType> returnType = method.returnType();
		if (returnType.isPresent()) {
			return returnType.get();
		} else {
			return new SVoidType();
		}
	}
}
