package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record SNewArrayExpression(
	SNormalType type,
	SExpression size,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
