package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;

import java.util.List;

public record MethodInvocationOp(
	Ident name,
	List<Expression> arguments
) implements PostfixOp {
}
