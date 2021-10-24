package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;

import java.util.Optional;

public record ReturnStatement(Optional<Expression> returnValue) implements Statement {
}
