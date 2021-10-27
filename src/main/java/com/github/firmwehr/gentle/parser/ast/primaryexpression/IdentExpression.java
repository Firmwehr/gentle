package com.github.firmwehr.gentle.parser.ast.primaryexpression;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;

public record IdentExpression(Ident name) implements PrimaryExpression {
}
