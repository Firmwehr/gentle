package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoAllocate(
	SExpression source,
	AllocateElementType type
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	@Override
	public String additionalInfo() {
		return type.name();
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
