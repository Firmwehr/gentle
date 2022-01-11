package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record DebugInfoMethodInvocation(
	SMethodInvocationExpression source,
	MethodInvocationElementType type
) implements HasDebugInformation {

	@Override
	public Optional<SourceSpan> debugSpan() {
		return source.debugSpan();
	}

	@Override
	public String additionalInfo() {
		return type.name();
	}

	public enum MethodInvocationElementType {
		ADDRESS,
		CALL,
		RESULTS_PROJ,
		RESULT_PROJ
	}

}
