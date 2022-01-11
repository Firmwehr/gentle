package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

public record DebugInfoAllocate(
	SExpression source,
	AllocateElementType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "allocate " + type + ": " + source.toDebugString();
	}

	public enum AllocateElementType {
		MEMBER_COUNT,
		TYPE_SIZE,
		ALLOCATE_ADDRESS,
		CALL,
		RESULTS_PROJ,
		RESULT_PROJ
	}
}
