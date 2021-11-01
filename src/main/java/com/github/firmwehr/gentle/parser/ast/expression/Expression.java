package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

import java.math.BigInteger;
import java.util.Arrays;

public sealed interface Expression extends PrettyPrint
	permits ArrayAccessExpression, BinaryOperatorExpression, BooleanLiteralExpression, FieldAccessExpression,
	        IdentExpression, IntegerLiteralExpression, LocalMethodCallExpression, MethodInvocationExpression,
	        NewArrayExpression, NewObjectExpression, NullExpression, ThisExpression, UnaryOperatorExpression {

	static BinaryOperatorExpression newBinOp(Expression lhs, Expression rhs, BinaryOperator operator) {
		return new BinaryOperatorExpression(lhs, rhs, operator);
	}

	static BooleanLiteralExpression newBool(boolean value) {
		return new BooleanLiteralExpression(value);
	}

	static IdentExpression newIdent(String name) {
		return new IdentExpression(Ident.dummy(name));
	}

	static IntegerLiteralExpression newInt(long value) {
		return new IntegerLiteralExpression(BigInteger.valueOf(value));
	}

	static LocalMethodCallExpression newCall(String name, Expression... arguments) {
		return new LocalMethodCallExpression(Ident.dummy(name), Arrays.asList(arguments));
	}

	static NewArrayExpression newNewArray(Type type, Expression size) {
		return new NewArrayExpression(type, size);
	}

	static NewObjectExpression newNewObject(String name) {
		return new NewObjectExpression(Ident.dummy(name));
	}

	static NullExpression newNull() {
		return new NullExpression();
	}

	static ThisExpression newThis() {
		return new ThisExpression();
	}

	default UnaryOperatorExpression withUnary(UnaryOperator operator) {
		return new UnaryOperatorExpression(operator, this);
	}

	default MethodInvocationExpression withCall(String name, Expression... arguments) {
		return new MethodInvocationExpression(this, Ident.dummy(name), Arrays.asList(arguments));
	}

	default ArrayAccessExpression withArrayAccess(Expression index) {
		return new ArrayAccessExpression(this, index);
	}

	default FieldAccessExpression withFieldAccess(String name) {
		return new FieldAccessExpression(this, Ident.dummy(name));
	}
}
