package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

public record SNewObjectExpression(
	SClassDeclaration classDecl
) implements SExpression {
	@Override
	public SExprType type() {
		return new SNormalType(new SClassType(classDecl));
	}
}
