package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record UnaryExpression<I>(SourcePosition position) implements Expression {
	
	public enum UnaryOperator {
		// FIXME: Copy operators from lexer
	}
}
