package com.github.firmwehr.gentle.semantic.ast.statement;

import com.github.firmwehr.gentle.semantic.ast.expression.SExpression;

public record SWhileStatement(
	SExpression condition,
	SStatement body
) implements SStatement {
}
