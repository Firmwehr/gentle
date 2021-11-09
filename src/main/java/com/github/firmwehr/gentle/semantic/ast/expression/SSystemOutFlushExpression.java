package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;

public record SSystemOutFlushExpression() implements SExpression {

	@Override
	public SExprType type() {
		return new SVoidType();
	}
}
