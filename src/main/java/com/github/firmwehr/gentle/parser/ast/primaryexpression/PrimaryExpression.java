package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface PrimaryExpression extends PrettyPrint
	permits NullExpression, BooleanLiteralExpression, IntegerLiteralExpression, IdentExpression,
	        LocalMethodCallExpression, ThisExpression, JustAnExpression, NewObjectExpression, NewArrayExpression {
}
