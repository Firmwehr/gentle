package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;

import java.util.List;

public record LocalMethodCallExpression(
	Ident name,
	List<Expression> arguments
) implements Expression {
}
