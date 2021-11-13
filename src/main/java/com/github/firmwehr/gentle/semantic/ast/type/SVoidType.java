package com.github.firmwehr.gentle.semantic.ast.type;

import java.util.Optional;

public record SVoidType() implements SExprType, SVoidyType {
	@Override
	public String format() {
		return "void";
	}

	@Override
	public SExprType asExprType() {
		return this;
	}

	@Override
	public Optional<SVoidType> asVoidType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableTo(SExprType other) {
		return false;
	}
}
