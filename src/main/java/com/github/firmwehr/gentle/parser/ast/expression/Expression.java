package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.math.BigInteger;
import java.util.Arrays;

public sealed interface Expression extends PrettyPrint
	permits ArrayAccessExpression, BinaryOperatorExpression, BooleanLiteralExpression, FieldAccessExpression,
	        IdentExpression, IntegerLiteralExpression, LocalMethodCallExpression, MethodInvocationExpression,
	        NewArrayExpression, NewObjectExpression, NullExpression, ThisExpression, UnaryOperatorExpression {

	SourceSpan sourceSpan();

	static BinaryOperatorExpression newBinOp(Expression lhs, Expression rhs, BinaryOperator operator) {
		return new BinaryOperatorExpression(lhs, rhs, operator, SourceSpan.dummy());
	}

	static BooleanLiteralExpression newBool(boolean value) {
		return new BooleanLiteralExpression(value, SourceSpan.dummy());
	}

	static IdentExpression newIdent(String name) {
		return new IdentExpression(Ident.dummy(name));
	}

	static IntegerLiteralExpression newInt(long value, boolean negated) {
		return new IntegerLiteralExpression(BigInteger.valueOf(value), negated, SourceSpan.dummy());
	}

	static IntegerLiteralExpression newInt(long value) {
		if (value < 0) {
			return newInt(-value, true);
		} else {
			return newInt(value, false);
		}
	}

	static LocalMethodCallExpression newCall(String name, Expression... arguments) {
		return new LocalMethodCallExpression(Ident.dummy(name), Arrays.asList(arguments), SourceSpan.dummy());
	}

	static NewArrayExpression newNewArray(Type type, Expression size) {
		return new NewArrayExpression(type, size, SourceSpan.dummy());
	}

	static NewObjectExpression newNewObject(String name) {
		return new NewObjectExpression(Ident.dummy(name), SourceSpan.dummy());
	}

	static NullExpression newNull() {
		return new NullExpression(SourceSpan.dummy());
	}

	static ThisExpression newThis() {
		return new ThisExpression(SourceSpan.dummy());
	}

	default UnaryOperatorExpression withUnary(UnaryOperator operator) {
		return new UnaryOperatorExpression(operator, this, SourceSpan.dummy());
	}

	default MethodInvocationExpression withCall(String name, Expression... arguments) {
		return new MethodInvocationExpression(this, Ident.dummy(name), Arrays.asList(arguments), SourceSpan.dummy(),
			SourceSpan.dummy());
	}

	default ArrayAccessExpression withArrayAccess(Expression index) {
		return new ArrayAccessExpression(this, index, SourceSpan.dummy());
	}

	default FieldAccessExpression withFieldAccess(String name) {
		return new FieldAccessExpression(this, Ident.dummy(name), SourceSpan.dummy());
	}
}
