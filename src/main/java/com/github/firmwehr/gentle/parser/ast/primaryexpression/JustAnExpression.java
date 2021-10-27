package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;

public record JustAnExpression(Expression expression) implements PrimaryExpression {
}
