package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.Expression;

import java.util.Optional;

public record IfStatement(
	Expression condition,
	Statement body,
	Optional<Statement> elseBody
) implements Statement {
}
