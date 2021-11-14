package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ExprWithParens;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Optional;

public record LocalVariableDeclarationStatement(
	Type type,
	Ident name,
	Optional<ExprWithParens> value
) implements BlockStatement {
	@Override
	public void prettyPrint(PrettyPrinter p, Parentheses parens) {
		p.add(type).add(" ").add(name);
		value.ifPresent(pair -> p.add(" = ").add(pair.expression(), Parentheses.OMIT));
		p.add(";");
	}
}
