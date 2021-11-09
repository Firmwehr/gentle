package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SType;

import java.util.Optional;

public record SLocalVariableExpression(
	LocalVariableDeclaration localVariable
) implements SExpression {
	@Override
	public Optional<SType> approximateType() {
		return Optional.of(localVariable.getType());
	}
}
