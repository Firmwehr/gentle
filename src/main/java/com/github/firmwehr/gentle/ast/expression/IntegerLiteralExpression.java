package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

public record IntegerLiteralExpression<I>(SourcePosition position, int value) implements Expression {
}
