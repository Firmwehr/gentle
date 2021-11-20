package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.Namespace;
import com.github.firmwehr.gentle.semantic.ast.basictype.SClassType;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;

/**
 * A declaration of a class.
 * <br>
 * This class uses <em>reference equality</em> semantics.
 */
public record SClassDeclaration(
	Ident name,
	Namespace<SField> fields,
	Namespace<SMethod> methods
) {

	public SNormalType type() {
		return new SNormalType(new SClassType(this));
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
