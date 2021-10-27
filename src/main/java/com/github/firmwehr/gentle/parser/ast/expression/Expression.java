package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface Expression extends PrettyPrint
	permits BinaryOperatorExpression, UnaryOperatorExpression, PostfixExpression {
}
