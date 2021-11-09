package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;

import java.util.Map;

public record SClassDeclaration(
	Ident name,
	Map<String, SField> fields,
	Map<String, SMethod> methods
) {
}
