package com.github.firmwehr.gentle.parser.ast.expression;

public sealed interface Expression
	permits NullExpression, BooleanLiteralExpression, IntegerLiteralExpression, IdentExpression,
	        LocalMethodCallExpression, ThisExpression, BinaryOperatorExpression, UnaryOperatorExpression,
	        PostfixOpExpression, NewObjectExpression, NewArrayExpression {
}
