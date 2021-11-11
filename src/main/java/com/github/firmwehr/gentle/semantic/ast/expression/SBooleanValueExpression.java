package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.basictype.SBooleanType;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record SBooleanValueExpression(
	boolean value,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public SExprType type() {
		return new SNormalType(new SBooleanType());
	}

	@Override
	public <T> Optional<T> accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
