package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record VariableAccessExpression<I>(SourcePosition position, I variableName) implements Expression<I> {
}