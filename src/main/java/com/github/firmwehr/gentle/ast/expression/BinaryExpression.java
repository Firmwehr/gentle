package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record BinaryExpression<I>(SourcePosition position, Expression<I> lhs, BinaryOperator operator, Expression<I> rhs) implements Expression<I> {
	
	public enum BinaryOperator {
		// FIXME: Copy operator types from lexer
	}
}