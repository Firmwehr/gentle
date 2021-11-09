package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

public record SBooleanValueExpression(
	boolean value
) implements SExpression {
	@Override
	public SExprType type() {
		return new SNormalType(new SBooleanType());
	}
}
