package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;

import java.util.Optional;

public sealed interface SExpression
	permits SArrayAccessExpression, SBinaryOperatorExpression, SBooleanValueExpression, SFieldAccessExpression,
	        SIntegerValueExpression, SLocalVariableExpression, SMethodInvocationExpression, SNewArrayExpression,
	        SNewObjectExpression, SNullExpression, SSystemInReadExpression, SSystemOutFlushExpression,
	        SSystemOutPrinlnExpression, SSystemOutWriteExpression, SThisExpression, SUnaryOperatorExpression {

	SExprType type();

	<T> Optional<T> accept(Visitor<T> visitor) throws SemanticException;
}
