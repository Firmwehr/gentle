package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.LocalVariableDeclaration;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

public record SLocalVariableExpression(
	LocalVariableDeclaration localVariable
) implements SExpression {
	@Override
	public SExprType type() {
		return localVariable.getType();
	}
}
