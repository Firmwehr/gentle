package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record NewObjectExpression<I>(SourcePosition position, I className) implements Expression<I> {
}
