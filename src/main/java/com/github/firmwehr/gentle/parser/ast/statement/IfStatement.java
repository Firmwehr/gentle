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
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		p.add("if (").add(condition, Parentheses.OMIT).add(")");

		boolean elseOnNewLine;
		if (body instanceof EmptyStatement) {
			p.add(" { }");
			elseOnNewLine = false;
		} else if (body instanceof Block) {
			p.add(" ").add(body);
			elseOnNewLine = false;
		} else {
			p.indent().newline().add(body).unindent();
			elseOnNewLine = true;
		}

		if (elseBody.isPresent()) {
			if (elseOnNewLine) {
				p.newline();
			} else {
				p.add(" ");
			}

			p.add("else");

			if (elseBody.get() instanceof EmptyStatement) {
				p.add(" { }");
			} else if (elseBody.get() instanceof IfStatement || elseBody.get() instanceof Block) {
				p.add(" ").add(elseBody.get());
			} else {
				p.indent().newline().add(elseBody.get()).unindent();
			}
		}
	}
}
