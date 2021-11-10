package com.github.firmwehr.gentle.semantic.ast.basictype;

import java.util.Optional;

public record SIntType() implements SBasicType {

	@Override
	public Optional<SIntType> asIntType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableFrom(SBasicType other) {
		return other.asIntType().isPresent();
	}
}
