package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.Namespace;

public record SClassDeclaration(
	Ident name,
	Namespace<SField> fields,
	Namespace<SMethod> methods
) {
}
