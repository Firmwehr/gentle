package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.debug.HasDebugInformation;
import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

/**
 * A declaration of a local variable or parameter.
 * <br>
 * This class uses <em>reference equality</em> semantics.
 */
public record LocalVariableDeclaration(
	SNormalType type,
	SourceSpan typeSpan,
	Ident declaration
) implements HasDebugInformation {
	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toDebugString() {
		return declaration.sourceSpan().toString();
	}
}
