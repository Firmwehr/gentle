package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record ArrayAccessExpression<I>(SourcePosition position, Expression<I> lhs, Expression<I> index) implements Expression<I> {
}
