package com.github.firmwehr.gentle.semantic.ast.basictype;

import java.util.Optional;

public record SBooleanType() implements SBasicType {
	@Override
	public String format() {
		return "boolean";
	}

	@Override
	public Optional<SBooleanType> asBooleanType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableFrom(SBasicType other) {
		return other.asBooleanType().isPresent();
	}
}
