package com.github.firmwehr.gentle.semantic.ast.basictype;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;

import java.util.Optional;

public record SClassType(SClassDeclaration classDecl) implements SBasicType {

	@Override
	public Optional<SClassType> asClassType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableFrom(SBasicType other) {
		// TODO: Is Singleton?
		return other.asClassType().map(classType -> classType.classDecl() == classDecl()).orElse(false);
	}
}
