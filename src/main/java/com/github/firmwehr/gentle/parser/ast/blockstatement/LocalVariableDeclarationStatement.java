package com.github.firmwehr.gentle.parser.ast.blockstatement;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.expression.Expression;
import com.github.firmwehr.gentle.parser.ast.type.Type;

import java.util.Optional;

public record LocalVariableDeclarationStatement(
	Type type,
	Ident name,
	Optional<Expression> value
) implements BlockStatement {
}
