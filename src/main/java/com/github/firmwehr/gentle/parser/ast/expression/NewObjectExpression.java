package com.github.firmwehr.gentle.parser.ast.expression;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record NewObjectExpression(Ident name) implements Expression {
}
