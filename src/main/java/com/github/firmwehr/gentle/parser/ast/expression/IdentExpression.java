package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record IdentExpression(Ident name) implements Expression {
}
