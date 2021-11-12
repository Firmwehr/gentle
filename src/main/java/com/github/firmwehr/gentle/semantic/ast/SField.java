package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

/**
 * A declaration of a field.
 * <br>
 * This class uses <em>reference equality</em> semantics.
 */
public record SField(
	SClassDeclaration classDecl,
	Ident name,
	SNormalType type,
	SourceSpan typeSpan
) {
	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}
