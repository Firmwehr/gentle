package com.github.firmwehr.gentle.semantic.ast.basictype;

import java.util.Optional;

public sealed interface SBasicType permits SBooleanType, SClassType, SIntType, SStringType {

	default Optional<SBooleanType> asBooleanType() {
		return Optional.empty();
	}

	default Optional<SClassType> asClassType() {
		return Optional.empty();
	}

	default Optional<SIntType> asIntType() {
		return Optional.empty();
	}

	default Optional<SStringType> asStringType() {
		return Optional.empty();
	}

	boolean isAssignableFrom(SBasicType other);
}
