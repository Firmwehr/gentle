package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.statement.SStatement;
import com.github.firmwehr.gentle.semantic.ast.type.SVoidyType;
import com.github.firmwehr.gentle.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * A declaration of a method.
 * <br>
 * This class uses <em>reference equality</em> semantics.
 */
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

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
