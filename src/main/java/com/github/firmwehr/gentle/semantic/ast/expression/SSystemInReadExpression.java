package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.basictype.SIntType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

public record SSystemInReadExpression() implements SExpression {
	@Override
	public SExprType type() {
		return new SNormalType(new SIntType());
	}
}
