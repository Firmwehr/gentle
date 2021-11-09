package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

public sealed interface SExpression
	permits SArrayAccessExpression, SBinaryOperatorExpression, SBooleanValueExpression, SFieldAccessExpression,
	        SIntegerValueExpression, SLocalVariableExpression, SMethodInvocationExpression, SNewArrayExpression,
	        SNewObjectExpression, SNullExpression, SSystemInReadExpression, SSystemOutFlushExpression,
	        SSystemOutPrinlnExpression, SSystemOutWriteExpression, SUnaryOperatorExpression {

	SExprType type();
}
