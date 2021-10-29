package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.semantic.ast.basictype.BasicType;
import com.google.common.base.Preconditions;

import java.util.Optional;

public record Type(
	BasicType basicType,
	int arrayLevel
) {
	public Type {
		Preconditions.checkArgument(arrayLevel >= 0);
	}

	public Optional<Type> withDecrementedLevel() {
		if (arrayLevel > 0) {
			return Optional.of(new Type(basicType, arrayLevel - 1));
		} else {

			return Optional.empty();
		}
	}
}
