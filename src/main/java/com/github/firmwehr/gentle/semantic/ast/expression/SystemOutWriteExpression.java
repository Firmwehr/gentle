package com.github.firmwehr.gentle.semantic.ast.expression;

public record SystemOutWriteExpression(
	Expression argument
) implements Expression {
}
