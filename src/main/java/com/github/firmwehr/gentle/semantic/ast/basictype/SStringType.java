package com.github.firmwehr.gentle.semantic.ast.basictype;

import java.util.Optional;

public record SStringType() implements SBasicType {
	@Override
	public String format() {
		return "String";
	}

	@Override
	public Optional<SStringType> asStringType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableFrom(SBasicType other) {
		return other.asStringType().isPresent();
	}
}
