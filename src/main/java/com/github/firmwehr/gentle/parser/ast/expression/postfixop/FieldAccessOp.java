package com.github.firmwehr.gentle.parser.ast.expression.postfixop;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record FieldAccessOp(Ident name) implements PostfixOp {
}
