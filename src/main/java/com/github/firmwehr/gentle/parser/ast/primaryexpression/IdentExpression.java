package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record IdentExpression(Ident name) implements PrimaryExpression {
}
