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
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		p.add("while (").add(condition, Parentheses.OMIT).add(")");

		if (body instanceof EmptyStatement) {
			p.add(body);
		} else if (body instanceof Block) {
			p.add(" ").add(body);
		} else {
			p.indent().newline().add(body).unindent();
		}
	}
}
