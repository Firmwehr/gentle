package com.github.firmwehr.gentle.parser.ast.type;

public sealed interface Type permits IntType, BooleanType, VoidType, IdentType, ArrayType {
}

