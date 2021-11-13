package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.UnaryOperator;
import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record SUnaryOperatorExpression(
	UnaryOperator operator,
	SExpression expression,
	SourceSpan sourceSpan
) implements SExpression {
	@Override
	public SExprType type() {
		return expression.type();
	}

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
