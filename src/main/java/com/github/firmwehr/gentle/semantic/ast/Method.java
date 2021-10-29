package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;

import java.util.List;
import java.util.Optional;

public record Method(
	ClassDeclaration classDecl,
	Ident name,
	Optional<Type> returnType,
	List<LocalVariableDeclaration> parameters,
	List<LocalVariableDeclaration> localVariables,
	Optional<LocalVariableDeclaration> thisKeyword,
	List<Statement> body
) {
}
