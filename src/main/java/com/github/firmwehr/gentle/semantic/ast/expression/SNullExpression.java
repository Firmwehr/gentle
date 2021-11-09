package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNullType;

public record SNullExpression() implements SExpression {
	@Override
	public SExprType type() {
		return new SNullType();
	}
}
