package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.ClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.Type;
import com.github.firmwehr.gentle.semantic.ast.basictype.ClassType;

import java.util.Optional;

public record NewObjectExpression(
	ClassDeclaration classDecl
) implements Expression {
	@Override
	public Optional<Type> approximateType() {
		return Optional.of(new Type(new ClassType(classDecl), 0));
	}
}
