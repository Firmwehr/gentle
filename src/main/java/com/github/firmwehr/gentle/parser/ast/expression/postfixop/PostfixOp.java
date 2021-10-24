package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

public sealed interface PostfixOp permits MethodInvocationOp, FieldAccessOp, ArrayAccessOp {
}
