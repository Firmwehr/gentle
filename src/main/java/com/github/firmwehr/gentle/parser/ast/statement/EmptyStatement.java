package com.github.firmwehr.gentle.parser.ast.statement;


import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record EmptyStatement() implements Statement, BlockStatement {
	@Override
	public BlockStatement asBlockStatement() {
		return this;
	}

	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		p.add(";");
	}
}
