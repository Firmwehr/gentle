package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record SWhileStatement(
	SExpression condition,
	SStatement body
) implements SStatement {

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}

	@Override
	public Optional<SourceSpan> debugSpan() {
		SourceSpan span = condition().sourceSpan();
		return Optional.of(SourceSpan.from(span, body.debugSpan().stream().toList()));
	}
}
