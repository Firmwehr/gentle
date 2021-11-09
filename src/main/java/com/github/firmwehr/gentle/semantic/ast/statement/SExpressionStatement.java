package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

public record SExpressionStatement(SExpression expression) implements SStatement {
}
