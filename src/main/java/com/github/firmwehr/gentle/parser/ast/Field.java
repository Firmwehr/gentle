package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.type.Type;

public record Field(
	Type type,
	Ident name
) {
}
