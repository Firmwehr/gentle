package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

import java.util.Arrays;

public sealed interface PrimaryExpression extends PrettyPrint
	permits NullExpression, BooleanLiteralExpression, IntegerLiteralExpression, IdentExpression,
	        LocalMethodCallExpression, ThisExpression, JustAnExpression, NewObjectExpression, NewArrayExpression {

	static BooleanLiteralExpression newBool(boolean value) {
		return new BooleanLiteralExpression(value);
	}

	static IdentExpression newIdent(String name) {
		return new IdentExpression(new Ident(name));
	}

	static IntegerLiteralExpression newInt(int value) {
		return new IntegerLiteralExpression(value);
	}

	static JustAnExpression fromExpr(Expression expression) {
		return new JustAnExpression(expression);
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
}
