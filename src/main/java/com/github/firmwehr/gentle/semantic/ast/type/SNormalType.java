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
	public String format() {
		return basicType.format() + "[]".repeat(arrayLevel);
	}

	@Override
	public SExprType asExprType() {
		return this;
	}

	@Override
	public Optional<SNormalType> asNormalType() {
		return Optional.of(this);
	}

	@Override
	public boolean isAssignableTo(SExprType other) {
		if (!(other instanceof SNormalType normalOther)) {
			return false;
		}
		if (normalOther.arrayLevel() != arrayLevel()) {
			return false;
		}
		return basicType().isAssignableFrom(normalOther.basicType());
	}

	public Optional<SNormalType> withDecrementedLevel() {
		if (arrayLevel > 0) {
			return Optional.of(new SNormalType(basicType, arrayLevel - 1));
		} else {

			return Optional.empty();
		}
	}
}
