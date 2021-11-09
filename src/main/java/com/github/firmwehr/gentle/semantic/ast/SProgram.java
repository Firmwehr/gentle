package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.semantic.Namespace;

public record SProgram(
	Namespace<SClassDeclaration> classes,
	SMethod mainMethod
) {
}
