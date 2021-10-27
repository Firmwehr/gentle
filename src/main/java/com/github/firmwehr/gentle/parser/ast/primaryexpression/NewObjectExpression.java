package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record NewObjectExpression(Ident name) implements PrimaryExpression {
}
