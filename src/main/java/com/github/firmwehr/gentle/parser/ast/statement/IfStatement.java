package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Optional;

public record IfStatement(
	Expression condition,
	Statement body,
	Optional<Statement> elseBody
) implements Statement, BlockStatement {
	@Override
	public BlockStatement asBlockStatement() {
		return this;
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("if ").add(condition).add(" ").add(body);
		elseBody.ifPresent(statement -> p.add(" else ").add(statement));
	}
}
