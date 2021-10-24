package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.type.Type;

public record MainMethod(
	Ident name,
	Type parameterType,
	Ident parameterName,
	Block block
) {
}
