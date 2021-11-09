package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.parser.ast.statement.Statement;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record SMethod(
	SClassDeclaration classDecl,
	Ident name,
	SVoidyType returnType,
	List<LocalVariableDeclaration> parameters,
	List<LocalVariableDeclaration> localVariables,
	Optional<LocalVariableDeclaration> thisKeyword,
	List<Statement> body
) {
	public static SMethod newMethod(
		SClassDeclaration classDecl, Ident name, SVoidyType returnType, List<LocalVariableDeclaration> parameters
	) {
		SNormalType type = new SNormalType(new SClassType(classDecl));
		LocalVariableDeclaration thisKeyword = new LocalVariableDeclaration(type, Optional.empty());

		return new SMethod(classDecl, name, returnType, parameters, new ArrayList<>(), Optional.of(thisKeyword),
			new ArrayList<>());
	}

	public static SMethod newMainMethod(
		SClassDeclaration classDecl, Ident name, SVoidyType returnType, List<LocalVariableDeclaration> parameters
	) {
		return new SMethod(classDecl, name, returnType, parameters, new ArrayList<>(), Optional.empty(),
			new ArrayList<>());
	}
}
