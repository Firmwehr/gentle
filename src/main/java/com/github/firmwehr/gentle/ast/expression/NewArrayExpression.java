package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;
import com.github.firmwehr.gentle.ast.type.ArrayType;

public record NewArrayExpression<I>(SourcePosition position, ArrayType<I> type, Expression<I> size) implements Expression<I> {
}
