package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record WhileStatement(
	Expression condition,
	Statement body
) implements Statement {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("while ").add(condition).add(" ").add(body);
	}
}
