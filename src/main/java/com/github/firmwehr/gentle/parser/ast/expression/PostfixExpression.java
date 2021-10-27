package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.PrimaryExpression;

import java.util.List;

public record PostfixExpression(
	PrimaryExpression expression,
	List<PostfixOp> postfixOps
) implements Expression {
}
