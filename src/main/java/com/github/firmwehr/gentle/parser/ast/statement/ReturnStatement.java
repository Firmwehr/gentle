package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.Optional;

public record ReturnStatement(
	Optional<Expression> returnValue,
	SourceSpan sourceSpan
) implements Statement, BlockStatement {
	@Override
	public BlockStatement asBlockStatement() {
		return this;
	}

	@Override
	public void prettyPrint(PrettyPrinter p, boolean omitParentheses) {
		p.add("return");
		returnValue.ifPresent(expression -> p.add(" ").add(expression, true));
		p.add(";");
	}
}
