package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.SMethod;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.List;

/**
 * @param postfixSpan span from the beginning of the name to the closing parenthesis
 */
public record SMethodInvocationExpression(
	SExpression expression,
	SMethod method,
	List<SExpression> arguments,
	SourceSpan postfixSpan,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public SExprType type() {
		return method.returnType().asExprType();
	}

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
