package com.github.firmwehr.gentle.semantic.ast;

import com.github.firmwehr.gentle.parser.ast.Ident;
import com.github.firmwehr.gentle.semantic.ast.type.SNormalType;
import com.github.firmwehr.gentle.source.SourceSpan;

public record SField(
	SClassDeclaration classDecl,
	Ident name,
	SNormalType type,
	SourceSpan typeSpan
) {
}
