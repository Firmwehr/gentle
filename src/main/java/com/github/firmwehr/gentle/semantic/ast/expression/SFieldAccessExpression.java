package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.SField;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record SFieldAccessExpression(
	SExpression expression,
	SField field,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public SExprType type() {
		return field.type();
	}

	@Override
	public <T> Optional<T> accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
