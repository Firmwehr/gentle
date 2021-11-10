package com.github.firmwehr.gentle.semantic.ast.type;

import java.util.Optional;

public sealed interface SExprType permits SNormalType, SNullType, SVoidType {

	boolean isAssignableTo(SExprType other);

	default Optional<SNormalType> asNormalType() {
		return Optional.empty();
	}

	default Optional<SNullType> asNullType() {
		return Optional.empty();
	}

	default Optional<SVoidType> asVoidType() {
		return Optional.empty();
	}
}
