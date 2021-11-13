package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.SClassDeclaration;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record SNewObjectExpression(
	SClassDeclaration classDecl,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public SExprType type() {
		return new SNormalType(new SClassType(classDecl));
	}

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
