package com.github.firmwehr.gentle.semantic.ast.type;

import java.util.Optional;

public record SNullType() implements SExprType {

	@Override
	public Optional<SNullType> asNullType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableTo(SExprType other) {
		if (other.asNormalType().isPresent() && other.asNormalType().get().arrayLevel() > 0) {
			return true;
		}
		return other.asClassType().isPresent() || other.asStringType().isPresent() || other.asNullType().isPresent();
	}
}
