package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record ThisExpression<I>(SourcePosition position) implements Expression<I> {
}
