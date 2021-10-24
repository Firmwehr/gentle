package com.github.firmwehr.gentle.parser.ast;

import com.github.firmwehr.gentle.parser.ast.statement.Block;
import com.github.firmwehr.gentle.parser.ast.type.Type;

import java.util.List;

public record Method(
	Type returnType,
	Ident name,
	List<Parameter> parameters,
	Block block
) {
}
