package com.github.firmwehr.gentle.parser.ast.blockstatement;

import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

public record JustAStatement(Statement statement) implements BlockStatement {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(statement);
	}
}
