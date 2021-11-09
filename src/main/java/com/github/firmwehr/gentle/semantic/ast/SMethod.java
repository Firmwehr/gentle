package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.List;
import java.util.Optional;

public record SMethod(
	SClassDeclaration classDecl,
	Ident name,
	Optional<SNormalType> returnType,
	List<LocalVariableDeclaration> parameters,
	List<LocalVariableDeclaration> localVariables,
	Optional<LocalVariableDeclaration> thisKeyword,
	List<Statement> body
) {
}
