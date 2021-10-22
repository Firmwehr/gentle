package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record FieldAccessExpression<I>(SourcePosition position, Expression<I> lhs, I fieldName) implements Expression<I> {
}
