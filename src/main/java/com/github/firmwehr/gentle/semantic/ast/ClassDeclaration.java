package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;

import java.util.Map;

public record ClassDeclaration(
	Ident name,
	Map<String, Field> fields,
	Map<String, Method> methods
) {
}
