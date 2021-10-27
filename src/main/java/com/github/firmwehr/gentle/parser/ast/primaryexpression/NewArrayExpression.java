package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.type.Type;

public record NewArrayExpression(
	Type type,
	Expression size
) implements PrimaryExpression {
}
