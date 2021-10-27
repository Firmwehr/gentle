package com.github.firmwehr.gentle.parser.ast.statement;


import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record EmptyStatement() implements Statement {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(";");
	}
}
