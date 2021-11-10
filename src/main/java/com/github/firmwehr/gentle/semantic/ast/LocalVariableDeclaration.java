package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

@SuppressWarnings("ClassCanBeRecord")
public class LocalVariableDeclaration {
	private final SNormalType type;
	private final Ident declaration;

	public LocalVariableDeclaration(SNormalType type, Ident declaration) {
		this.type = type;
		this.declaration = declaration;
	}

	public SNormalType getType() {
		return type;
	}

	public Ident getDeclaration() {
		return declaration;
	}
}
