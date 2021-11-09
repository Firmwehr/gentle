package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.SType;

import java.util.Optional;

public sealed interface SExpression
	permits SArrayAccessExpression, SBinaryOperatorExpression, SBooleanValueExpression, SFieldAccessExpression,
	        SIntegerValueExpression, SLocalVariableExpression, SMethodInvocationExpression, SNewArrayExpression,
	        SNewObjectExpression, SNullExpression, SSystemInReadExpression, SSystemOutFlushExpression,
	        SSystemOutPrinlnExpression, SSystemOutWriteExpression, SUnaryOperatorExpression {

	/**
	 * Approximate an expression's type for name resolution.
	 *
	 * @return the expression's type, or Optional.empty() if no useful type could be derived
	 */
	default Optional<SType> approximateType() {
		return Optional.empty();
	}
}
