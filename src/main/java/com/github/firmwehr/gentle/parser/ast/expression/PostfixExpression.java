package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.ArrayAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.FieldAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.MethodInvocationOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.PrimaryExpression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public record PostfixExpression(
	PrimaryExpression expression,
	List<PostfixOp> postfixOps
) implements Expression {
	public PostfixExpression withOp(PostfixOp op) {
		List<PostfixOp> newOps = Stream.concat(postfixOps.stream(), Stream.of(op)).toList();
		return new PostfixExpression(expression, newOps);
	}

	public PostfixExpression withCall(String name, Expression... arguments) {
		return withOp(new MethodInvocationOp(new Ident(name), Arrays.asList(arguments)));
	}

	public PostfixExpression withArrayAccess(Expression index) {
		return withOp(new ArrayAccessOp(index));
	}

	public PostfixExpression withFieldAccess(String name) {
		return withOp(new FieldAccessOp(new Ident(name)));
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(expression).addAll(postfixOps, "", false);
	}
}
