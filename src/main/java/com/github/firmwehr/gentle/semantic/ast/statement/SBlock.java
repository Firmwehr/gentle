package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;

import java.util.List;
import java.util.Optional;

public record SBlock(List<SStatement> statements) implements SStatement {
	public SBlock() {
		this(List.of());
	}

	@Override
	public <T> Optional<T> accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
