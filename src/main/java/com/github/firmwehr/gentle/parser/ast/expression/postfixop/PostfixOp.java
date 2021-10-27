package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrint;

public sealed interface PostfixOp extends PrettyPrint permits MethodInvocationOp, FieldAccessOp, ArrayAccessOp {
}
