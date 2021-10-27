package com.github.firmwehr.gentle.parser.ast.expression;

public sealed interface Expression permits BinaryOperatorExpression, UnaryOperatorExpression, PostfixExpression {
}
