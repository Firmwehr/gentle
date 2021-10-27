package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;

import java.util.List;

public record LocalMethodCallExpression(
	Ident name,
	List<Expression> arguments
) implements PrimaryExpression {
}
