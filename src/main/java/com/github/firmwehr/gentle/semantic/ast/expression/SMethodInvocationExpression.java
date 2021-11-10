package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

import java.util.List;
import java.util.Optional;

public record SMethodInvocationExpression(
	SExpression expression,
	SMethod method,
	List<SExpression> arguments
) implements SExpression {
	@Override
	public SExprType type() {
		return method.returnType().asExprType();
	}

	@Override
	public <T> Optional<T> accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
