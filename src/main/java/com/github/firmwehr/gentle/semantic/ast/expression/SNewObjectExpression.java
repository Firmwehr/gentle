package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.Optional;

public record SNewObjectExpression(
	SClassDeclaration classDecl
) implements SExpression {
	@Override
	public Optional<SNormalType> approximateType() {
		return Optional.of(new SNormalType(new SClassType(classDecl), 0));
	}
}
