package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

import java.util.Optional;

public record SReturnStatement(Optional<SExpression> returnValue) implements SStatement {
}
