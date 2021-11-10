package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.Optional;

public record SNewArrayExpression(
	SNormalType type,
	SExpression size
) implements SExpression {
	@Override
	public <T> Optional<T> accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
