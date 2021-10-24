package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;

import java.util.List;

public record PostfixOpExpression(
	Expression expression,
	List<PostfixOp> postfixOps
) implements Expression {
}
