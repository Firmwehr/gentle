package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;

public record SMethod(
	SClassDeclaration classDecl,
	boolean isStatic,
	Ident name,
	SVoidyType returnType,
	SourceSpan returnTypeSpan,
	List<LocalVariableDeclaration> parameters,
	List<SStatement> body
) {
	public static SMethod newMethod(
		SClassDeclaration classDecl,
		boolean isStatic,
		Ident name,
		SVoidyType returnType,
		SourceSpan returnTypeSpan,
		List<LocalVariableDeclaration> parameters
	) {
		return new SMethod(classDecl, isStatic, name, returnType, returnTypeSpan, parameters, new ArrayList<>());
	}
}
