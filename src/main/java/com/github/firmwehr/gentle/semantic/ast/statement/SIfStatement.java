package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record SIfStatement(
	SExpression condition,
	SStatement body,
	Optional<SStatement> elseBody
) implements SStatement {
	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}

	@Override
	public Optional<SourceSpan> debugSpan() {
		SourceSpan span = condition().sourceSpan();
		List<SourceSpan> spans = Stream.of(Optional.of(body), elseBody)
			.flatMap(Optional::stream)
			.flatMap(it -> it.debugSpan().stream())
			.toList();

		return Optional.of(SourceSpan.from(span, spans));
	}
}
