package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SFieldAccessExpression;

public record DebugInfoFieldAccess(
	SFieldAccessExpression source,
	FieldAccessElementType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "field access " + type + ": " + source.toDebugString();
	}

	public enum FieldAccessElementType {
		MEMBER,
		LOAD,
		LOAD_RESULT
	}
}
