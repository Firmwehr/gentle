package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.Expression;

public record ExpressionStatement(Expression expression) implements Statement {
}
