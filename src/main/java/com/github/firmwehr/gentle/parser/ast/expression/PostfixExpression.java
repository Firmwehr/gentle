package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.PrimaryExpression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record PostfixExpression(
	PrimaryExpression expression,
	List<PostfixOp> postfixOps
) implements Expression {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(expression).addAll(postfixOps, "", false);
	}
}
