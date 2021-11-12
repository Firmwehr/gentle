package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

/**
 * A declaration of a local variable or parameter.
 * <br>
 * This class uses <em>reference equality</em> semantics.
 */
public class LocalVariableDeclaration {
	private final SNormalType type;
	private final SourceSpan typeSpan;
	private final Ident declaration;

	public LocalVariableDeclaration(
		SNormalType type, SourceSpan typeSpan, Ident declaration
	) {
		this.type = type;
		this.typeSpan = typeSpan;
		this.declaration = declaration;
	}

	public SNormalType getType() {
		return type;
	}

	public SourceSpan getTypeSpan() {
		return typeSpan;
	}

	public Ident getDeclaration() {
		return declaration;
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
