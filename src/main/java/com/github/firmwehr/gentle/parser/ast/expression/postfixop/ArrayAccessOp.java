package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;

public record ArrayAccessOp(Expression index) implements PostfixOp {
}
