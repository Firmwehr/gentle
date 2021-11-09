package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class LocalVariableDeclaration {
	private final SNormalType type;
	private final Optional<Ident> declaration;

	public LocalVariableDeclaration(SNormalType type, Optional<Ident> declaration) {
		this.type = type;
		this.declaration = declaration;
	}

	public SNormalType getType() {
		return type;
	}

	public Optional<Ident> getDeclaration() {
		return declaration;
	}

	public boolean isThis() {
		return declaration.isEmpty();
	}
}
