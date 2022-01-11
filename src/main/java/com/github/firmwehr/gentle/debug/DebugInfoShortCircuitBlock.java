package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SBinaryOperatorExpression;

public record DebugInfoShortCircuitBlock(
	SBinaryOperatorExpression source,
	ShortCircuitBlockType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "Short-circuit " + type + ": " + source.toDebugString();
	}

	public enum ShortCircuitBlockType {
		OR,
		AND
	}
}
