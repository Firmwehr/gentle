package com.github.firmwehr.gentle.parser.ast.primaryexpression;

public sealed interface PrimaryExpression
	permits NullExpression, BooleanLiteralExpression, IntegerLiteralExpression, IdentExpression,
	        LocalMethodCallExpression, ThisExpression, JustAnExpression, NewObjectExpression, NewArrayExpression {
}
