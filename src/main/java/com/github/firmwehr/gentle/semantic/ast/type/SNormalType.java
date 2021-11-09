package com.github.firmwehr.gentle.semantic.ast.type;

import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.google.common.base.Preconditions;

import java.util.Optional;

public record SNormalType(
	SBasicType basicType,
	int arrayLevel
) implements SExprType {
	public SNormalType {
		Preconditions.checkArgument(arrayLevel >= 0);
	}

	public Optional<SNormalType> withDecrementedLevel() {
		if (arrayLevel > 0) {
			return Optional.of(new SNormalType(basicType, arrayLevel - 1));
		} else {

			return Optional.empty();
		}
	}
}
