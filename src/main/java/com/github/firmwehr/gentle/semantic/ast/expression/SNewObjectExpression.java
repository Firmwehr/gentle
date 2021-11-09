package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.SType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;

import java.util.Optional;

public record SNewObjectExpression(
	SClassDeclaration classDecl
) implements SExpression {
	@Override
	public Optional<SType> approximateType() {
		return Optional.of(new SType(new SClassType(classDecl), 0));
	}
}
