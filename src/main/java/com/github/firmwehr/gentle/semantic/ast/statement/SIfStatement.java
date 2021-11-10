package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

import java.util.Optional;

public record SIfStatement(
	SExpression condition,
	SStatement body,
	Optional<SStatement> elseBody
) implements SStatement {
	@Override
	public <T> Optional<T> accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
