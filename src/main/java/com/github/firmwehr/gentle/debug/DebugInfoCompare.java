package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoCompare(
	SExpression source,
	CompareElementType type
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source().debugSpan();
	}

	@Override
	public String additionalInfo() {
		return HasDebugInformation.super.additionalInfo();
	}

	public enum CompareElementType {
		COMPARE,
		COND,
		TRUE_PROJ,
		FALSE_PROJ
	}

}
