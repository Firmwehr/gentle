package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record BooleanLiteralExpression<I>(SourcePosition position, boolean value) implements Expression<I> {
}
