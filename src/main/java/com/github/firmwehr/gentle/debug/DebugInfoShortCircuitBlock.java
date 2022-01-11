package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoShortCircuitBlock(
	SBinaryOperatorExpression source,
	ShortCircuitBlockType type
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	@Override
	public String additionalInfo() {
		return type.name();
	}

	public enum ShortCircuitBlockType {
		OR,
		AND
	}
}
