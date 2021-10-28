package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.ArrayAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.FieldAccessOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.MethodInvocationOp;
import com.github.firmwehr.gentle.parser.ast.expression.postfixop.PostfixOp;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

import java.util.Arrays;

public sealed interface Expression extends PrettyPrint
	permits BinaryOperatorExpression, BooleanLiteralExpression, IdentExpression, IntegerLiteralExpression,
	        LocalMethodCallExpression, NewArrayExpression, NewObjectExpression, NullExpression, PostfixExpression,
	        ThisExpression, UnaryOperatorExpression {

	static BinaryOperatorExpression newBinOp(Expression lhs, Expression rhs, BinaryOperator operator) {
		return new BinaryOperatorExpression(lhs, rhs, operator);
	}

	static BooleanLiteralExpression newBool(boolean value) {
		return new BooleanLiteralExpression(value);
	}

	static IdentExpression newIdent(String name) {
		return new IdentExpression(new Ident(name));
	}

	static IntegerLiteralExpression newInt(int value) {
		return new IntegerLiteralExpression(value);
	}

	static LocalMethodCallExpression newCall(String name, Expression... arguments) {
		return new LocalMethodCallExpression(new Ident(name), Arrays.asList(arguments));
	}

	static NewArrayExpression newNewArray(Type type, Expression size) {
		return new NewArrayExpression(type, size);
	}

	static NewObjectExpression newNewObject(String name) {
		return new NewObjectExpression(new Ident(name));
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

	default PostfixExpression withPostfix(PostfixOp operator) {
		return new PostfixExpression(this, operator);
	}

	default PostfixExpression withCall(String name, Expression... arguments) {
		return withPostfix(new MethodInvocationOp(new Ident(name), Arrays.asList(arguments)));
	}

	default PostfixExpression withArrayAccess(Expression index) {
		return withPostfix(new ArrayAccessOp(index));
	}

	default PostfixExpression withFieldAccess(String name) {
		return withPostfix(new FieldAccessOp(new Ident(name)));
	}
}
