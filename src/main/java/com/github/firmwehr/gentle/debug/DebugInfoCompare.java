package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

public record DebugInfoCompare(
	SExpression source,
	CompareElementType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "cmp " + type + ": " + source.toDebugString();
	}

	public enum CompareElementType {
		COMPARE,
		COND,
		TRUE_PROJ,
		FALSE_PROJ
	}

}
