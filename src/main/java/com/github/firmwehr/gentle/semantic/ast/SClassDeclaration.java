package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.Namespace;

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

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
