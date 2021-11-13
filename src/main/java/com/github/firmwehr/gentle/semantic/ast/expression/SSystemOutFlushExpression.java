package com.github.firmwehr.gentle.semantic.ast.expression;

import com.github.firmwehr.gentle.semantic.SemanticException;
import com.github.firmwehr.gentle.semantic.Visitor;
import com.github.firmwehr.gentle.semantic.ast.type.SExprType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record SSystemOutFlushExpression(SourceSpan sourceSpan) implements SExpression {

	@Override
	public SExprType type() {
		return new SVoidType();
	}

	@Override
	public <T> T accept(Visitor<T> visitor) throws SemanticException {
		return visitor.visit(this);
	}
}
