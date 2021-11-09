package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

import java.util.Optional;

public record SIfStatement(
	SExpression condition,
	SStatement body,
	Optional<SStatement> elseBody
) implements SStatement {
}
