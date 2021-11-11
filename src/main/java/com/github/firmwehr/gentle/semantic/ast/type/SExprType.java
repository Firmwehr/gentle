package com.github.firmwehr.gentle.semantic.ast.type;

import com.github.firmwehr.gentle.semantic.ast.basictype.SBasicType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.basictype.SStringType;

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

	default Optional<SBasicType> asBasicType() {
		return asNormalType().filter(t -> t.arrayLevel() == 0).map(SNormalType::basicType);
	}

	default Optional<SBooleanType> asBooleanType() {
		return asBasicType().flatMap(SBasicType::asBooleanType);
	}

	default Optional<SClassType> asClassType() {
		return asBasicType().flatMap(SBasicType::asClassType);
	}

	default Optional<SIntType> asIntType() {
		return asBasicType().flatMap(SBasicType::asIntType);
	}

	default Optional<SStringType> asStringType() {
		return asBasicType().flatMap(SBasicType::asStringType);
	}
}
