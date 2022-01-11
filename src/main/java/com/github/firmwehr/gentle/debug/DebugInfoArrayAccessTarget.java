package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoArrayAccessTarget(
	SArrayAccessExpression source,
	ArrayAccessTargetType targetType
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	@Override
	public String additionalInfo() {
		return targetType.toString();
	}

	public enum ArrayAccessTargetType {
		TYPE_SIZE,
		OFFSET,
		RESULT_COMPUTATION,
		RESULT_CONVERSION
	}
}
