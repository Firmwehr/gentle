package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record WhileStatement(
	Expression condition,
	Statement body
) implements Statement, BlockStatement {
	@Override
	public BlockStatement asBlockStatement() {
		return this;
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add("while (").add(condition, true).add(") ").add(body);
	}
}
