package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.Expression;

import java.util.Optional;

public record ReturnStatement(Optional<Expression> returnValue) implements Statement {
}
