package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.debug.HasDebugInformation;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public sealed interface SExpression extends HasDebugInformation
	permits SArrayAccessExpression, SBinaryOperatorExpression, SBooleanValueExpression, SFieldAccessExpression,
	        SIntegerValueExpression, SLocalVariableExpression, SMethodInvocationExpression, SNewArrayExpression,
	        SNewObjectExpression, SNullExpression, SSystemInReadExpression, SSystemOutFlushExpression,
	        SSystemOutPrintlnExpression, SSystemOutWriteExpression, SThisExpression, SUnaryOperatorExpression {

	SourceSpan sourceSpan();

	SExprType type();

	<T> T accept(Visitor<T> visitor) throws SemanticException;

	@Override
	default Optional<SourceSpan> debugSpan() {
		return Optional.ofNullable(sourceSpan());
	}

	@Override
	default String additionalInfo() {
		return "Type: " + type().format();
	}
}
