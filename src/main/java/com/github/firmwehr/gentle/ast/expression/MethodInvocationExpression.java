package com.github.firmwehr.gentle.ast.expression;

import com.github.firmwehr.gentle.ast.SourcePosition;

import java.util.List;

public record MethodInvocationExpression<I>(SourcePosition position, Expression<I> lhs, I methodName, List<Expression<I>> arguments) implements Expression<I> {
}
