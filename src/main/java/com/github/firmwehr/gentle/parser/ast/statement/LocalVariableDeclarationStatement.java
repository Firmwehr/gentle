package com.github.firmwehr.gentle.parser.ast.statement;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.Type;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.prettyprint.PrettyPrinter;

import java.util.Optional;

public record LocalVariableDeclarationStatement(
	Type type,
	Ident name,
	Optional<Expression> value
) implements BlockStatement {
	@Override
	public void prettyPrint(PrettyPrinter p) {
		p.add(type).add(" ").add(name);
		value.ifPresent(expression -> p.add(" = ").add(expression));
		p.add(";");
	}
}
