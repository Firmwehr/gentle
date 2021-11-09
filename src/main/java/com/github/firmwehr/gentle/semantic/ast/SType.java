package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.google.common.base.Preconditions;

import java.util.Optional;

public record SType(
	SBasicType basicType,
	int arrayLevel
) {
	public SType {
		Preconditions.checkArgument(arrayLevel >= 0);
	}

	public Optional<SType> withDecrementedLevel() {
		if (arrayLevel > 0) {
			return Optional.of(new SType(basicType, arrayLevel - 1));
		} else {

			return Optional.empty();
		}
	}
}
