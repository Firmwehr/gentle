package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.semantic.Namespace;

/**
 * A complete program consisting of a set of classes and a main method.
 * <br>
 * This class uses <em>reference equality</em> semantics.
 */
public record SProgram(
	Namespace<SClassDeclaration> classes,
	SMethod mainMethod
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
