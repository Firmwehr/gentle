package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;

import java.util.Optional;

public record IfStatement(
	Expression condition,
	Statement body,
	Optional<Statement> elseBody
) implements Statement {
}
