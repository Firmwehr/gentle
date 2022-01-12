package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.List;
import java.util.Optional;

public record SBlock(List<SStatement> statements) implements SStatement {
	public SBlock() {
		this(List.of());
	}

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}

	@Override
	public Optional<SourceSpan> debugSpan() {
		List<SourceSpan> spans = statements.stream().flatMap(it -> it.debugSpan().stream()).toList();
		if (spans.isEmpty()) {
			return Optional.empty();
		}
		SourceSpan span = spans.get(0);
		if (spans.size() == 1) {
			return Optional.ofNullable(span);
		}
		return Optional.of(SourceSpan.from(span, spans));
	}
}
