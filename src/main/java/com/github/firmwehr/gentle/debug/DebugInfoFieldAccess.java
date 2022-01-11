package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoFieldAccess(
	SFieldAccessExpression source,
	FieldAccessElementType type
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	@Override
	public String additionalInfo() {
		return type().name();
	}

	public enum FieldAccessElementType {
		MEMBER,
		LOAD,
		LOAD_RESULT
	}
}
