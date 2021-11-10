package com.github.firmwehr.gentle.semantic.ast.type;

import java.util.Optional;

public record SNullType() implements SExprType {

	@Override
	public Optional<SNullType> asNullType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableTo(SExprType other) {
		if (other instanceof SNormalType type) {
			return type.basicType().asClassType().isPresent() || type.basicType().asStringType().isPresent();
		}
		return other instanceof SNullType;
	}
}
