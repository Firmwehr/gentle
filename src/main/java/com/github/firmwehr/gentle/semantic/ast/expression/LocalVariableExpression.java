package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.Type;

import java.util.Optional;

public record LocalVariableExpression(
	LocalVariableDeclaration localVariable
) implements Expression {
	@Override
	public Optional<Type> approximateType() {
		return Optional.of(localVariable.getType());
	}
}
