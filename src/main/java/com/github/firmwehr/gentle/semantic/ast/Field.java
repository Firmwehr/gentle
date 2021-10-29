package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;

public record Field(
	ClassDeclaration classDecl,
	Ident name,
	Type type
) {
}
