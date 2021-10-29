package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;

import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class LocalVariableDeclaration {
	private final Type type;
	private final Optional<Ident> declaration;

	public LocalVariableDeclaration(Type type, Optional<Ident> declaration) {
		this.type = type;
		this.declaration = declaration;
	}

	public Type getType() {
		return type;
	}

	public Optional<Ident> getDeclaration() {
		return declaration;
	}

	public boolean isThis() {
		return declaration.isEmpty();
	}
}
