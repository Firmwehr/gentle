package com.github.firmwehr.gentle.debug;

import com.github.firmwehr.gentle.semantic.ast.expression.SMethodInvocationExpression;

public record DebugInfoMethodInvocation(
	SMethodInvocationExpression source,
	MethodInvocationElementType type
) implements HasDebugInformation {

	@Override
	public String toDebugString() {
		return "Method call " + type + ": " + source.toDebugString();
	}

	public enum MethodInvocationElementType {
		ADDRESS,
		CALL,
		RESULTS_PROJ,
		RESULT_PROJ
	}

}
