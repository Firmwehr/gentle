package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Optional;

public record ReturnStatement(Optional<Expression> returnValue) implements Statement, BlockStatement {
	@Override
	public BlockStatement asBlockStatement() {
		return this;
	}

	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("return");
		returnValue.ifPresent(expression -> p.add(" ").add(expression));
		p.add(";");
	}
}
