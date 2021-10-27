package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.blockstatement.BlockStatement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.List;

public record Block(List<BlockStatement> statements) implements Statement {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add("{").indent().addAll(statements, "", true).unindent().add("}");
	}
}
