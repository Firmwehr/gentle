package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record SField(
	SClassDeclaration classDecl,
	Ident name,
	SType type
) {
}
