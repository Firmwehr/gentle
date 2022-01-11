package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SArrayAccessExpression;

public record DebugInfoArrayAccessTarget(
	SArrayAccessExpression source,
	ArrayAccessTargetType targetType
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "array access " + targetType + ": " + source.toDebugString();
	}

	public enum ArrayAccessTargetType {
		TYPE_SIZE,
		OFFSET,
		RESULT_COMPUTATION,
		RESULT_CONVERSION
	}
}
