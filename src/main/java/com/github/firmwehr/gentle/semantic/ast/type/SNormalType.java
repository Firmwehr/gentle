package com.github.firmwehr.gentle.semantic.ast.type;

import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.google.common.base.Preconditions;

import java.util.Optional;

public record SNormalType(
	SBasicType basicType,
	int arrayLevel
) implements SExprType, SVoidyType {
	public SNormalType {
		Preconditions.checkArgument(arrayLevel >= 0);
	}

	public SNormalType(SBasicType basicType) {
		this(basicType, 0);
	}

	@Override
	public SExprType asExprType() {
		return this;
	}

	public Optional<SNormalType> withDecrementedLevel() {
		if (arrayLevel > 0) {
			return Optional.of(new SNormalType(basicType, arrayLevel - 1));
		} else {

			return Optional.empty();
		}
	}
}
