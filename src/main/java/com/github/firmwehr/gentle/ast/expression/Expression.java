package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.HasSourcePosition;

public sealed interface Expression<I> extends HasSourcePosition
		permits
		NullLiteralExpression,
		BooleanLiteralExpression,
		IntegerLiteralExpression,
		ThisExpression,
		UnaryExpression,
		BinaryExpression,
		MethodInvocationExpression,
		FieldAccessExpression,
		VariableAccessExpression,
		ArrayAccessExpression,
		NewObjectExpression,
		NewArrayExpression {
}
