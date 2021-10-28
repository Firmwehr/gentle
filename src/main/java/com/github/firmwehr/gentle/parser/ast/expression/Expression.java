package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.primaryexpression.PrimaryExpression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

import java.util.List;

public sealed interface Expression extends PrettyPrint
	permits BinaryOperatorExpression, UnaryOperatorExpression, PostfixExpression {

	static BinaryOperatorExpression newBinOp(Expression lhs, Expression rhs, BinaryOperator operator) {
		return new BinaryOperatorExpression(lhs, rhs, operator);
	}

	static UnaryOperatorExpression newUnOp(UnaryOperator operator, Expression expression) {
		return new UnaryOperatorExpression(operator, expression);
	}

	static PostfixExpression fromPrimary(PrimaryExpression expression) {
		return new PostfixExpression(expression, List.of());
	}

	static PostfixExpression newBool(boolean value) {
		return fromPrimary(PrimaryExpression.newBool(value));
	}

	static PostfixExpression newIdent(String name) {
		return fromPrimary(PrimaryExpression.newIdent(name));
	}

	static PostfixExpression newInt(int value) {
		return fromPrimary(PrimaryExpression.newInt(value));
	}

	static PostfixExpression newCall(String name, Expression... arguments) {
		return fromPrimary(PrimaryExpression.newCall(name, arguments));
	}

	static PostfixExpression newNewArray(Type type, Expression size) {
		return fromPrimary(PrimaryExpression.newNewArray(type, size));
	}

	static PostfixExpression newNewObject(String name) {
		return fromPrimary(PrimaryExpression.newNewObject(name));
	}

	static PostfixExpression newNull() {
		return fromPrimary(PrimaryExpression.newNull());
	}

	static PostfixExpression newThis() {
		return fromPrimary(PrimaryExpression.newThis());
	}
}
