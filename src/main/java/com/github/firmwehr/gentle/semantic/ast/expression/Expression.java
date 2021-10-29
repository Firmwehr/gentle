package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.Type;

import java.util.Optional;

public sealed interface Expression
	permits ArrayAccessExpression, BinaryOperatorExpression, BooleanValueExpression, FieldAccessExpression,
	        IntegerValueExpression, LocalVariableExpression, MethodInvocationExpression, NewArrayExpression,
	        NewObjectExpression, NullExpression, SystemInReadExpression, SystemOutFlushExpression,
	        SystemOutPrinlnExpression, SystemOutWriteExpression, UnaryOperatorExpression {

	/**
	 * Approximate an expression's type for name resolution.
	 *
	 * @return the expression's type, or Optional.empty() if no useful type could be derived
	 */
	default Optional<Type> approximateType() {
		return Optional.empty();
	}
}
