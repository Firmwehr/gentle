package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;

public record ExpressionStatement(Expression expression) implements Statement {
}
